package it.unipi.aide.iot.mqtt;

import com.google.gson.Gson;
import it.unipi.aide.iot.bean.HumiditySample;
import it.unipi.aide.iot.bean.LightIntensitySample;
import it.unipi.aide.iot.bean.TemperatureSample;
import it.unipi.aide.iot.coap.devices.LightSystem;
import it.unipi.aide.iot.coap.devices.VentilationSystem;
import it.unipi.aide.iot.coap.devices.WateringSystem;
import it.unipi.aide.iot.config.ManualControl;
import it.unipi.aide.iot.mqtt.sensors.HumidityDevice;
import it.unipi.aide.iot.mqtt.sensors.LightIntensityDevice;
import it.unipi.aide.iot.mqtt.sensors.TemperatureDevice;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;

public class MQTTHandler implements MqttCallback {

    private MqttClient mqttClient = null;
    private final TemperatureDevice temperatureDevice;
    private final HumidityDevice humidityDevice;
    private final LightIntensityDevice lightIntensityDevice;
    private Gson parser=new Gson();

    private final String BROKER = "tcp://127.0.0.1:1883";
    private final String CLIENT_ID = "RemoteControlApp";


    public MQTTHandler() {
        temperatureDevice = new TemperatureDevice();
        lightIntensityDevice = new LightIntensityDevice();
        humidityDevice = new HumidityDevice();

        do {
            try {
                mqttClient = new MqttClient(BROKER, CLIENT_ID);
                System.out.println("Connecting to the broker: " + BROKER);
                mqttClient.setCallback(this);
                connectBroker();
            }
            catch(MqttException me)
            {
                System.out.println("Connection error");
            }
        }while(!mqttClient.isConnected());

    }

    private void connectBroker () throws MqttException {
        mqttClient.connect();
        mqttClient.subscribe(humidityDevice.HUMIDITY_TOPIC);
        System.out.println("Subscribed to: " + humidityDevice.HUMIDITY_TOPIC);
        mqttClient.subscribe(lightIntensityDevice.LIGHTINTENSITY_TOPIC);
        System.out.println("Subscribed to: " + lightIntensityDevice.LIGHTINTENSITY_TOPIC);
        mqttClient.subscribe(temperatureDevice.TEMPERATURE_TOPIC);
        System.out.println("Subscribed to: " + temperatureDevice.TEMPERATURE_TOPIC);
    }

    @Override
    public void connectionLost(Throwable throwable){
        System.out.println("Connection with the Broker lost!");
        System.out.println(throwable);
        // We have lost the connection, we have to try to reconnect after waiting some time
        // At each iteration we increase the time waited
        int iter = 0;
        do {
            iter++; // first iteration iter=1
            int MAX_RECONNECTION_ITER = 10;
            if (iter > MAX_RECONNECTION_ITER)
            {
                System.err.println("Reconnection with the broker not possible!");
                System.exit(-1);
            }
            try
            {
                int SECONDS_TO_WAIT_FOR_RECONNECTION = 5;
                Thread.sleep((long) SECONDS_TO_WAIT_FOR_RECONNECTION * 1000 * iter);
                System.out.println("New attempt to connect to the broker...");
                connectBroker();
            }
            catch (MqttException | InterruptedException e)
            {
                e.printStackTrace();
            }
        } while (!this.mqttClient.isConnected());
        System.out.println("Connection with the Broker restored!");

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws MqttException {
        String payload = new String(mqttMessage.getPayload());

        if (topic.equals(temperatureDevice.TEMPERATURE_TOPIC)){
            TemperatureSample temperatureSample = parser.fromJson(payload, TemperatureSample.class);
            temperatureDevice.addSample(temperatureSample);
            float temperatureAvg = temperatureDevice.getAvgTemperature();
            if (temperatureAvg < temperatureDevice.getLowerBound() && VentilationSystem.getState().equals("OFF")) {
                VentilationSystem.switchVentilationSystem("INC");
                mqttClient.publish("temperature-command", new MqttMessage("INC".getBytes(StandardCharsets.UTF_8)));
                System.out.println("The current temperature is too low: " + temperatureAvg + "°C, increase it");
            }
            else if(temperatureAvg > temperatureDevice.getUpperBound() && VentilationSystem.getState().equals("OFF")){
                VentilationSystem.switchVentilationSystem("DEC");
                mqttClient.publish("temperature-command", new MqttMessage("DEC".getBytes(StandardCharsets.UTF_8)));
                System.out.println("The current temperature is too high: " + temperatureAvg + "°C, decrease it");
            }
            else if((VentilationSystem.getState().equals("INC") && temperatureAvg > temperatureDevice.getUpperBound()) || (VentilationSystem.getState().equals("DEC") && temperatureAvg < temperatureDevice.getLowerBound())){
                VentilationSystem.switchVentilationSystem("OFF");
                mqttClient.publish("temperature-command", new MqttMessage("OFF".getBytes(StandardCharsets.UTF_8)));
                System.out.println("Temperature level is in the range, ventilation system is off");
                ManualControl.setManualVentilation(false);
            }
            else if((VentilationSystem.getState().equals("INC") && temperatureAvg > (temperatureDevice.getLowerBound()+temperatureDevice.getUpperBound())/2) ||
                    (VentilationSystem.getState().equals("DEC") && temperatureAvg < (temperatureDevice.getLowerBound()+temperatureDevice.getUpperBound())/2)){
                VentilationSystem.switchVentilationSystem("OFF");
                ManualControl.setManualVentilation(false);
                mqttClient.publish("temperature-command", new MqttMessage("OFF".getBytes(StandardCharsets.UTF_8)));
                System.out.println("Temperature level is in the range, ventilation system is off");
            }
        }

        else if(topic.equals(humidityDevice.HUMIDITY_TOPIC)){
            HumiditySample humiditySample = parser.fromJson(payload, HumiditySample.class);
            humidityDevice.addSample(humiditySample);
            float humidityAvg = humidityDevice.getAvgHumidity();
            if (humidityAvg < humidityDevice.getLowerBound() && WateringSystem.getState().equals("OFF")) {
                WateringSystem.switchWateringSystem(true);
                mqttClient.publish("humidity-command", new MqttMessage("ON".getBytes(StandardCharsets.UTF_8)));
                System.out.println("The current humidity is too low: " + humidityAvg + " increase it");
            }
            else if(ManualControl.getManualWatering() && humidityAvg > humidityDevice.getUpperBound() && WateringSystem.getState().equals("ON")){
                WateringSystem.switchWateringSystem(false);
                ManualControl.setManualWatering(false);
                mqttClient.publish("humidity-command", new MqttMessage("OFF".getBytes(StandardCharsets.UTF_8)));
                System.out.println("The current humidity is too high: " + humidityAvg + "decrease it");
            }
            else if(!ManualControl.getManualWatering() && WateringSystem.getState().equals("ON") && humidityAvg > (humidityDevice.getLowerBound()+humidityDevice.getUpperBound())/2){
                WateringSystem.switchWateringSystem(false);
                mqttClient.publish("humidity-command", new MqttMessage("OFF".getBytes(StandardCharsets.UTF_8)));
                System.out.println("Humidity level is in the range, watering system is off");
            }

        }

        else if(topic.equals(lightIntensityDevice.LIGHTINTENSITY_TOPIC)){
            LightIntensitySample lightSample = parser.fromJson(payload, LightIntensitySample.class);
            lightIntensityDevice.addSample(lightSample);
            int lastSample=lightSample.getIntensity();

            if ((lastSample > lightIntensityDevice.getUpperBound() || lastSample < lightIntensityDevice.getLowerBound()) && (LightSystem.getState().equals("OFF"))) {
                LightSystem.switchLightSystem(true);
                mqttClient.publish("light-command", new MqttMessage("ON".getBytes(StandardCharsets.UTF_8)));
                System.out.println("The current ambient light is out of range: " + lightSample.getIntensity() + " adjust it");
            }
            else if((LightSystem.getState().equals("ON")) &&
                    (lastSample >= lightIntensityDevice.getLowerBound() && lastSample <= lightIntensityDevice.getUpperBound())){
                LightSystem.switchLightSystem(false);
                mqttClient.publish("light-command", new MqttMessage("OFF".getBytes(StandardCharsets.UTF_8)));
                System.out.println("ambient light level is in the range, light system is off");
            }

        }else{
            System.out.println("You are not subscribed to the '" + topic + "' topic");
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    public MqttClient getMqttClient() {
        return mqttClient;
    }

    public TemperatureDevice getTemperatureDevice(){
        return temperatureDevice;
    }

       public HumidityDevice getHumidityDevice() {
        return humidityDevice;
    }

    public LightIntensityDevice getLightIntensityDevice() {
        return lightIntensityDevice;
    }
}
