package it.unipi.aide.iot;

import it.unipi.aide.iot.coap.CoapRegistrationServer;
import it.unipi.aide.iot.coap.devices.LightSystem;
import it.unipi.aide.iot.coap.devices.VentilationSystem;
import it.unipi.aide.iot.coap.devices.WateringSystem;
import it.unipi.aide.iot.config.ManualControl;
import it.unipi.aide.iot.mqtt.MQTTHandler;
import it.unipi.aide.iot.mqtt.sensors.HumidityDevice;
import it.unipi.aide.iot.mqtt.sensors.LightIntensityDevice;
import it.unipi.aide.iot.mqtt.sensors.TemperatureDevice;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;

import it.unipi.aide.iot.persistence.DBDriver;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class GreenHouseControlSystem {
    public static void main(String[] args) throws SQLException {
        System.out.println("START");
        CoapRegistrationServer coapRegistrationServer = new CoapRegistrationServer();
        coapRegistrationServer.start();
        MQTTHandler mqttHandler = new MQTTHandler();
        MqttClient mqttClient = mqttHandler.getMqttClient();

        DBDriver mySqlDbHandler = DBDriver.getInstance();
        mySqlDbHandler.getConnection();

        printAvailableCommands();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String command;
        String[] arguments;
        while (true) {
            try {
                command = bufferedReader.readLine();
                arguments = command.split(" ");
                switch(arguments[0]){
                    case "!get_temperature":
                        getCurrentTemperature(mqttHandler);
                        break;
                    case "!set_temperature":
                        setTemperatureBounds(arguments, mqttHandler);
                        break;
                    case "!active_ventilation":
                        activeVentilation(arguments, mqttClient);
                        break;
                    case "!deactivate_ventilation":
                        deactivateVentilation(mqttClient);
                        break;
                    case "!get_humidity":
                        getCurrentHumidityLevel(mqttHandler);
                        break;
                    case "!set_humidity":
                        setHumidityBounds(arguments, mqttHandler);
                        break;
                    case "!start_watering":
                        activateWatering(mqttClient);
                        break;
                    case "!stop_watering":
                        deactivateWatering(mqttClient);
                        break;
                    case "!get_light_intensity":
                        getCurrentIntensityLevel(mqttHandler);
                        break;
                    case "!set_light_intensity":
                        setIntensityBounds(arguments, mqttHandler);
                        break;
                    case "!activate_lightning" :
                        activateLightning(mqttClient);
                        break;
                    case "!stop_lightning" :
                        deactivateLightning(mqttClient);
                        break;
                    case "!exit":
                        System.out.println("Bye!");
                        coapRegistrationServer.stop();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Command not valid, try again!\n");
                        break;
                }
            }
            catch (IOException | MqttException e) {
                e.printStackTrace();
            }
        }

    }

     private static void getCurrentIntensityLevel(MQTTHandler mqttHandler) {
         System.out.println("Current light intensity level is: " +  mqttHandler.getLightIntensityDevice().getCurrentIntensityLevel()+ " lux");
    }

    private static void setIntensityBounds(String[] arguments, MQTTHandler mqttHandler) {
         if (arguments.length != 3){
            System.out.println("Are required only two arguments!");
            return;
        }
        float lowerBound = Float.parseFloat(arguments[1]);
        float upperBound = Float.parseFloat(arguments[2]);
        if(upperBound < lowerBound) {
            System.out.println("The upper bound is lower than the lower bound\n");
            return;
        }
        LightIntensityDevice lightIntensityDevice = mqttHandler.getLightIntensityDevice();
        lightIntensityDevice.setLowerBound(lowerBound);
        lightIntensityDevice.setUpperBound(upperBound);
        System.out.println("New light intensity thresholds registered");
    }

    private static void activateLightning(MqttClient mqttClient) throws MqttException {
        if (!Objects.equals(LightSystem.getState(), "OFF")) {
            System.out.println("Light system already active");
            ManualControl.setManualLightning(true);
        } else{
            ManualControl.setManualLightning(true);
            LightSystem.switchLightSystem(true);
            mqttClient.publish("light-command", new MqttMessage("ON".getBytes(StandardCharsets.UTF_8)));
            System.out.println("Lightning system is ON");
         }   
    }

    private static void deactivateLightning(MqttClient mqttClient) throws MqttException {
         if(Objects.equals(LightSystem.getState(), "OFF")) {
            System.out.println("Light system is already off");
        }
        else {
            LightSystem.switchLightSystem(false);
            mqttClient.publish("light-command", new MqttMessage("OFF".getBytes(StandardCharsets.UTF_8)));
            System.out.println("Light system switched OFF");
        }
        ManualControl.setManualLightning(false);
    }

    private static void getCurrentTemperature(MQTTHandler mqttHandler) {
        System.out.println("Current temperature is: " +  mqttHandler.getTemperatureDevice().getCurrentTemperature() + "°C");
    }

    private static void setTemperatureBounds(String[] arguments, MQTTHandler mqttHandler) {
        if (arguments.length != 3){
            System.out.println("Are required only two arguments!");
            return;
        }
        float lowerBound = Float.parseFloat(arguments[1]);
        float upperBound = Float.parseFloat(arguments[2]);
        if(upperBound < lowerBound) {
            System.out.println("The upper bound is lower than the lower bound\n");
            return;
        }
        TemperatureDevice temperatureDevice=mqttHandler.getTemperatureDevice();
        temperatureDevice.setLowerBound(lowerBound);
        temperatureDevice.setUpperBound(upperBound);
        System.out.println("New temperature thresholds registered");
    }

    private static void activeVentilation(String[] arguments, MqttClient mqttClient) throws MqttException {
        if (arguments.length != 2){
            System.out.println("Missing argument/s in the request"); 
            return;
        }
        if (!Objects.equals(VentilationSystem.getState(), "OFF")) {
            System.out.println("Ventilation system already active");
            ManualControl.setManualVentilation(true);
        } 
        else 
        {
            if (!Objects.equals(arguments[1], "INC") & !Objects.equals(arguments[1], "DEC")) {
                System.out.println("Not valid mode");
            }
            ManualControl.setManualVentilation(true);
            VentilationSystem.switchVentilationSystem(arguments[1]);
            mqttClient.publish("temperature-command", new MqttMessage(arguments[1].getBytes(StandardCharsets.UTF_8)));
            System.out.println("Ventilation system started in " + arguments[1] + " mode");

        }
    }

    private static void deactivateVentilation(MqttClient mqttClient) throws MqttException {
        if(Objects.equals(VentilationSystem.getState(), "OFF")) {
            System.out.println("Ventilation system is already off");
        }
        else 
        {
            VentilationSystem.switchVentilationSystem("OFF");
            mqttClient.publish("temperature-command", new MqttMessage("OFF".getBytes(StandardCharsets.UTF_8)));
            System.out.println("Ventilation system switched OFF");
        }
        ManualControl.setManualVentilation(false);
    }

    private static void getCurrentHumidityLevel(MQTTHandler mqttHandler) {
         System.out.println("Current humidity level is: " +  mqttHandler.getHumidityDevice().getCurrentHumidityLevel() + " level");
    }

     private static void setHumidityBounds(String[] arguments, MQTTHandler mqttHandler) {
        if (arguments.length != 3){
            System.out.println("Are required only two arguments!");
            return;
        }
        float lowerBound = Float.parseFloat(arguments[1]);
        float upperBound = Float.parseFloat(arguments[2]);
        if(upperBound < lowerBound) {
            System.out.println("The upper bound is lower than the lower bound\n");
            return;
        }
        HumidityDevice humidityDevice = mqttHandler.getHumidityDevice();
        humidityDevice.setLowerBound(lowerBound);
        humidityDevice.setUpperBound(upperBound);
        System.out.println("New humidity thresholds registered");
    }

    private static void activateWatering(MqttClient mqttClient) throws MqttException{
        if (!Objects.equals(WateringSystem.getState(), "OFF")) {
            System.out.println("Watering system already active");
            ManualControl.setManualWatering(true);

        } else{
            WateringSystem.switchWateringSystem(true);
            mqttClient.publish("humidity-command", new MqttMessage("ON".getBytes(StandardCharsets.UTF_8)));
            System.out.println("Watering system started in ON mode");
        }
    }

    private static void deactivateWatering(MqttClient mqttClient) throws MqttException {
         if(Objects.equals(WateringSystem.getState(), "OFF")) {
            System.out.println("Watering system is already off");
        }
        else {
            WateringSystem.switchWateringSystem(false);
            mqttClient.publish("humidity-command", new MqttMessage("OFF".getBytes(StandardCharsets.UTF_8)));
            System.out.println("Watering system switched OFF");
        }
    }

    private static void printAvailableCommands() {
        System.out.println("***************************** SMART GREENHOUSE *****************************\n" +
                "The following commands are available:\n" +
                "1) !help <command> --> shows the details of a command\n" +
                "2) !get_humidity --> recovers the last humidity measurement\n" +
                "3) !set_humidity <lower bound> <upper bound> --> sets the range within which the humidity must stay\n" +
                "4) !start_watering --> starts watering system\n" +
                "5) !stop_watering --> stops watering system\n" +
                "6) !get_temperature --> recovers the last temperature measurement\n" +
                "7) !set_temperature <lower bound> <upper_bound> --> sets the range within which the temperature must stay\n" +
                "8) !active_ventilation <mode> --> starts ventilation system\n" +
                "9) !deactivate_ventilation --> stops ventilation system\n" +
                "10) !get_light_intensity --> recovers the last light intensity measurement\n" + 
                "11) !set_light_intensity <lower bound> <upper bound> --> \n" +
                "12) !activate_lightning --> starts lightning system\n"+
                "13) !stop_lightning --> stops lightning system\n" +
                "14) !exit --> terminates the program\n"
        );
    }

}
