/*
 * Copyright (c) 2020, Carlo Vallati, University of Pisa
  * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*---------------------------------------------------------------------------*/
#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "os/sys/log.h"
#include "os/dev/button-hal.h"

#include <string.h>
#include <sys/node-id.h>
/*---------------------------------------------------------------------------*/
#define LOG_MODULE "temperature-sensor"
#ifdef MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

/*---------------------------------------------------------------------------*/
/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Defaukt config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)


// We assume that the broker does not require authentication


/*---------------------------------------------------------------------------*/
/* Various states */
static uint8_t state;

#define STATE_INIT    		  0
#define STATE_NET_OK    	  1
#define STATE_CONNECTING      2
#define STATE_CONNECTED       3
#define STATE_SUBSCRIBED      4
#define STATE_DISCONNECTED    5

/*---------------------------------------------------------------------------*/
PROCESS_NAME(mqtt_client_temp);
AUTOSTART_PROCESSES(&mqtt_client_temp);

/*---------------------------------------------------------------------------*/
/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN   64
/*---------------------------------------------------------------------------*/
/*
 * Buffers for Client ID and Topics.
 * Make sure they are large enough to hold the entire respective string
 */
#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
//static char sub_topic[BUFFER_SIZE];

static struct mqtt_connection conn;

#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND)
static struct etimer periodic_timer;

mqtt_status_t status;
char broker_address[CONFIG_IP_ADDR_STR_LEN];

#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

static struct mqtt_message *msg_ptr = 0;

static bool inc_temp = false;
static bool dec_temp = false;
static int temp = 25; // we cannot use float value in the testbed
static int variation = 0;
button_hal_button_t *btn;

/*---------------------------------------------------------------------------*/

PROCESS(mqtt_client_temp, "MQTT Client-Temperature");


/*---------------------------------------------------------------------------*/
static void
pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk,
            uint16_t chunk_len)
{
  printf("Pub Handler: topic='%s' (len=%u), chunk_len=%u\n", topic,
          topic_len, chunk_len);

  if(strcmp(topic, "temperature-command") == 0) {
    printf("Received Actuator command\n");
		if(strcmp((const char*) chunk, "INC") == 0) {
			printf("Turn ON the ventilation system to increase temperature\n");
			inc_temp = true;
			dec_temp = false;	
		} else if(strcmp((const char*) chunk, "DEC") == 0) {
			printf("Turn ON the ventilation system to decrease temperature\n");	
			inc_temp = false;
			dec_temp = true;
		} else if(strcmp((const char*) chunk, "OFF") == 0)  {
			printf("Turn OFF the ventilation system\n");	
			inc_temp = false;
			dec_temp = false;
		}	
  }else{
    printf("Topic not valid!\n");
  }
}
/*---------------------------------------------------------------------------*/
static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data){
  switch(event) {
  case MQTT_EVENT_CONNECTED: {
    printf("Application has a MQTT connection\n");

    state = STATE_CONNECTED;
    break;
  }
  case MQTT_EVENT_DISCONNECTED: {
    printf("MQTT Disconnect. Reason %u\n", *((mqtt_event_t *)data));

    state = STATE_DISCONNECTED;
    process_poll(&mqtt_client_temp);
    break;
  }
  case MQTT_EVENT_PUBLISH: {
    msg_ptr = data;

    pub_handler(msg_ptr->topic, strlen(msg_ptr->topic),
                msg_ptr->payload_chunk, msg_ptr->payload_length);
    break;
  }
  case MQTT_EVENT_SUBACK: {
      #if MQTT_311
          mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;

          if(suback_event->success) {
            printf("Application is subscribed to topic successfully\n");
          } else {
            printf("Application failed to subscribe to topic (ret code %x)\n", suback_event->return_code);
          }
      #else
          printf("Application is subscribed to topic successfully\n");
      #endif
          break;
  }
  case MQTT_EVENT_UNSUBACK: {
    printf("Application is unsubscribed to topic successfully\n");
    break;
  }
  case MQTT_EVENT_PUBACK: {
    printf("Publishing complete.\n");
    break;
  }
  default:
    printf("Application got a unhandled MQTT event: %i\n", event);
    break;
  }
}

static bool have_connectivity(void){
  if(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL) {
    return false;
  }
  return true;
}

/*---------------------------------------------------------------------------*/
PROCESS_THREAD(mqtt_client_temp, ev, data){

  PROCESS_BEGIN();
  
  printf("MQTT Client Process\n");

  // Initialize the ClientID as MAC address
  snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
                     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
                     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
                     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

  btn = button_hal_get_by_index(0);
  if(btn == NULL) {
    LOG_ERR("Unable to find button 0... exit");
    PROCESS_EXIT();
  }

  // Broker registration					 
  mqtt_register(&conn, &mqtt_client_temp, client_id, mqtt_event,
                  MAX_TCP_SEGMENT_SIZE);
				  
  state=STATE_INIT;
				    
  // Initialize periodic timer to check the status 
  etimer_set(&periodic_timer, STATE_MACHINE_PERIODIC);

  /* Main loop */
  while(1) {

    PROCESS_YIELD();

    if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || 
	      ev == PROCESS_EVENT_POLL){
			  			  
		  if(state==STATE_INIT ){
			 if(have_connectivity()==true)  
				 state = STATE_NET_OK;
		  } 
		  
		  if(state == STATE_NET_OK){
			  // Connect to MQTT server
			  printf("Connecting!\n");
			  
			  memcpy(broker_address, broker_ip, strlen(broker_ip));
			  
			  mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT,
						   (DEFAULT_PUBLISH_INTERVAL * 3) / CLOCK_SECOND,
						   MQTT_CLEAN_SESSION_ON);
			  state = STATE_CONNECTING;
		  }
		  
		  if(state==STATE_CONNECTED){
		  
			  // Subscribe to a topic
			  status = mqtt_subscribe(&conn, NULL, "temperature-command", MQTT_QOS_LEVEL_0);

			  printf("Subscribing!\n");
			  if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
                  LOG_ERR("Tried to subscribe but command queue was full!\n");
                  PROCESS_EXIT();
			  }
			  
			  state = STATE_SUBSCRIBED;
		  }

			  
		if(state == STATE_SUBSCRIBED){
        // Publish something
            sprintf(pub_topic, "%s", "temperature");
        
      // simulate the behavior of the sensor				
			if (inc_temp || dec_temp) {
				variation = rand()%3; 	// a value in [0,2]
				temp = (inc_temp) ? (temp + variation) : (temp - variation);
			} else {
				if((rand()%10) < 6) { // 60% chance that the temperature will change
					variation = (rand()%5)-2; // a value in [-2, 2]
					temp = temp + variation;
				}
			}

			printf("New value of temperature: %d\n", temp);
				
			sprintf(app_buffer, "{\"nodeId\": %d, \"temperature\": %d}",node_id, temp);
          
      mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer,
                strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
		
		} else if ( state == STATE_DISCONNECTED ){
		   LOG_ERR("Disconnected form MQTT broker\n");	
		   state = STATE_INIT;
		}
		
		etimer_set(&periodic_timer, STATE_MACHINE_PERIODIC);
      
    }else if(ev == button_hal_press_event){
          if(inc_temp)
			      inc_temp = false;
	    	  else inc_temp = true;
            
    }
    else if (ev == PROCESS_EVENT_EXIT) {
      mqtt_disconnect(&conn);
    }
    else if (ev == PROCESS_EVENT_CONTINUE) {
      printf("MQTT client connection failed\n");
    }

  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
