package it.unipi.aide.iot.coap.devices;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import it.unipi.aide.iot.persistence.DBDriver;

public class LightSystem {
    private static final List<CoapClient> clientLightSystemList = new ArrayList<>();
    private static String state = "OFF";

    public static String getState(){
        return state;
    }

    public static void switchLightSystem(boolean mode){
        if(clientLightSystemList.isEmpty())
            return;

        String msg = "mode=" + (mode ? "ON" : "OFF");
        state=(mode ? "ON" : "OFF");

        for(CoapClient client: clientLightSystemList) {
            client.put(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse coapResponse) {
                    if (coapResponse != null) {
                        if (!coapResponse.isSuccess())
                            System.out.print("[ERROR]Light system switch: PUT request unsuccessful\n");
                    }
                }

                @Override
                public void onError() {
                    System.err.print("[ERROR] Light system switch " + client.getURI() + "]");
                }
            }, msg, MediaTypeRegistry.TEXT_PLAIN);
        }
    }

    public void registerLightSystem(String ip){
        clientLightSystemList.add(new CoapClient("coap://[" + ip + "]/light-system/switch"));
        DBDriver.getInstance().insertActuator(ip, "light system");
    }

    public void unregisterLightSystem(String ip) {
        CoapClient foundClient = null;

        for (CoapClient client : clientLightSystemList) {
            if (client.getURI().equals(ip)) {
                foundClient = client;
                break;
            }
        }
        if(foundClient != null) {
            clientLightSystemList.remove(foundClient);
            DBDriver.getInstance().removeActuator(ip, "light system");
            System.out.println("[REMOVE] The light system device [" + ip + "] is now removed\n");
        } else {
            System.out.println("[NOT REMOVE] The light system device [" + ip + "] not found in database\n");
        }
    }
    
}
