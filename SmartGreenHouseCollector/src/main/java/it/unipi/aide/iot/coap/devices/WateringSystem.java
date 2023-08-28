package it.unipi.aide.iot.coap.devices;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import it.unipi.aide.iot.persistence.DBDriver;

public class WateringSystem {
      private static final List<CoapClient> clientWateringSystemList = new ArrayList<>();
    private static String state = "OFF";

    public static String getState(){
        return state;
    }

    public static void switchWateringSystem(boolean mode){
        if(clientWateringSystemList.isEmpty())
            return;

        String msg = "mode=" + (mode ? "ON" : "OFF");
        state=(mode ? "ON" : "OFF");

        for(CoapClient client: clientWateringSystemList) {
            client.put(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse coapResponse) {
                    if (coapResponse != null) {
                        if (!coapResponse.isSuccess())
                            System.out.print("[ERROR]Watering system switch: PUT request unsuccessful\n");
                    }
                }

                @Override
                public void onError() {
                    System.err.print("[ERROR] Watering system switch " + client.getURI() + "]");
                }
            }, msg, MediaTypeRegistry.TEXT_PLAIN);
        }
    }

    public void registerWateringSystem(String ip){
        clientWateringSystemList.add(new CoapClient("coap://[" + ip + "]/watering-system/switch"));
        DBDriver.getInstance().insertActuator(ip, "watering system");
    }

    public void unregisterWateringSystem(String ip) {
        CoapClient foundClient = null;

        for (CoapClient client : clientWateringSystemList) {
            if (client.getURI().equals(ip)) {
                foundClient = client;
                break;
            }
        }
        if(foundClient != null) {
            clientWateringSystemList.remove(foundClient);
            DBDriver.getInstance().removeActuator(ip, "watering system");
            System.out.println("[REMOVE] The watering system device [" + ip + "] is now removed\n");
        } else {
            System.out.println("[NOT REMOVE] The watering system device [" + ip + "] not found in database\n");
        }
    }
    
}
