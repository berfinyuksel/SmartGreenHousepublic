package it.unipi.aide.iot.coap.devices;

import it.unipi.aide.iot.persistence.DBDriver;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import java.util.*;

public class VentilationSystem {
    private static final List<CoapClient> clientVentilationSystemList = new ArrayList<>();
    private static String state = "OFF";

    public static String getState(){
        return state;
    }

    public static void switchVentilationSystem(String mode){
        if(clientVentilationSystemList.isEmpty())
            return;

        if (Objects.equals(mode, "INC"))
            state = "INC";
        else if (Objects.equals(mode, "DEC"))
            state = "DEC";
        else
            state = "OFF";

        String msg = "mode=" + mode;
        for(CoapClient client: clientVentilationSystemList) {
            client.put(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse coapResponse) {
                    if (coapResponse != null) {
                        if (!coapResponse.isSuccess())
                            System.out.print("[ERROR]Ventilation system switch: PUT request unsuccessful\n");
                    }
                }

                @Override
                public void onError() {
                    System.err.print("[ERROR] Ventilation system switch " + client.getURI() + "]");
                }
            }, msg, MediaTypeRegistry.TEXT_PLAIN);
        }
    }

    public void registerVentilationSystem(String ip){
        clientVentilationSystemList.add(new CoapClient("coap://[" + ip + "]/ventilation-system/switch"));
        DBDriver.getInstance().insertActuator(ip, "ventilation system");
    }

    public void unregisterVentilationSystem(String ip) {
        CoapClient foundClient = null;

        for (CoapClient client : clientVentilationSystemList) {
            if (client.getURI().equals(ip)) {
                foundClient = client;
                break;
            }
        }
        if(foundClient != null) {
            clientVentilationSystemList.remove(foundClient);
            DBDriver.getInstance().removeActuator(ip, "ventilation system");
            System.out.println("[REMOVE] The ventilation system device [" + ip + "] is now removed\n");
        } else {
            System.out.println("[NOT REMOVE] The ventilation system device [" + ip + "] not found in database\n");
        }
    }

}
