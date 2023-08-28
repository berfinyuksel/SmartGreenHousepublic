package it.unipi.aide.iot.coap;

import it.unipi.aide.iot.coap.devices.LightSystem;
import it.unipi.aide.iot.coap.devices.VentilationSystem;
import it.unipi.aide.iot.coap.devices.WateringSystem;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.nio.charset.StandardCharsets;

public class CoapRegistrationServer  extends CoapServer {
    private final VentilationSystem ventilationSystem = new VentilationSystem();
    private final WateringSystem wateringSystem = new WateringSystem();
    private final LightSystem lightSystem = new LightSystem();

    public CoapRegistrationServer() {
        CoapResource registrationResource = new CoapResource("registration") {

            @Override
            public void handleGET(CoapExchange exchange) {
                Response response = new Response(CoAP.ResponseCode.CONTENT);
                System.out.println(response);
            }

            @Override
            public void handlePOST(CoapExchange exchange) {
                String deviceType = exchange.getRequestText();
                String ip = exchange.getSourceAddress().getHostAddress();
                boolean success = true;

                switch (deviceType) {
                    case "ventilation_system":
                        ventilationSystem.registerVentilationSystem(ip);
                        break;
                    case "watering_system":
                        wateringSystem.registerWateringSystem(ip);
                        break;
                    case "light_system":
                        lightSystem.registerLightSystem(ip);
                        break;
                    default:
                        success = false;
                        break;
                }

                if (success)
                    exchange.respond(CoAP.ResponseCode.CREATED, "Success".getBytes(StandardCharsets.UTF_8));
                else
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Unsuccessful".getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void handleDELETE(CoapExchange exchange) {
                String deviceType = exchange.getRequestText();
                String ip = exchange.getSourceAddress().getHostAddress();
                boolean success = true;

                switch (deviceType) {
                    case "ventilation system":
                        ventilationSystem.unregisterVentilationSystem(ip);
                        break;
                    case "light system":
                        lightSystem.unregisterLightSystem(ip);
                        break;
                    case "watering system":
                        wateringSystem.unregisterWateringSystem(ip);
                        break;
                    default:
                        success = false;
                        break;
                }

                if (success)
                    exchange.respond(CoAP.ResponseCode.DELETED, "Device removed from DB".getBytes(StandardCharsets.UTF_8));
                else
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Unsuccessful".getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void handlePUT(CoapExchange exchange) {}
        };

        registrationResource.setObservable(true);
        this.add(registrationResource);
    }
}
