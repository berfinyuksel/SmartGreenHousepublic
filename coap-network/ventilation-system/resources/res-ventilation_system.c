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
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of the Contiki operating system.
 */

#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "ventilation-system"
#define LOG_LEVEL LOG_LEVEL_APP

static void ventilation_sys_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_ventilation_system,
         "title=\"Ventilation System\";rt=\"Control\"",
         NULL,
         NULL,
         ventilation_sys_put_handler,
         NULL);

bool ventilation_sys_on = false;

static void ventilation_sys_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;
	char mode[4];
	memset(mode, 0, 3);
	
	int mode_success = 1;
	
	len = coap_get_post_variable(request, "mode", &text);
	memcpy(mode, text, len);
	if(len > 0 && len < 10) {
		if(strncmp(text, "INC", len) == 0) {
			leds_set(LEDS_GREEN);
			ventilation_sys_on = true;
			LOG_INFO("Ventilation system INC mode \n");
		} else if(strncmp(text, "DEC", len) == 0) {
			leds_set(LEDS_GREEN);
			ventilation_sys_on = true;
			LOG_INFO("Ventilation System DEC mode \n");
		} else if(strncmp(text, "OFF", len) == 0) {
			leds_set(LEDS_RED);
			ventilation_sys_on = false;
			LOG_INFO("Ventilation System OFF\n");
		} else {
			mode_success = 0;
		}
	} else {
		mode_success = 0;
	}

	
	if(!mode_success) {
    		coap_set_status_code(response, BAD_REQUEST_4_00);
 	}
}