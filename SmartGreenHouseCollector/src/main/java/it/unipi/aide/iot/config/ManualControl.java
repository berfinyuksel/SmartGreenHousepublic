package it.unipi.aide.iot.config;

public class ManualControl {
    private static boolean manualVentilation=false;
    private static boolean ManualLightning=false;
    private static boolean ManualWatering=false;

    public ManualControl(){

    }

    public static boolean getManualVentilation(){
        return manualVentilation;
    }

    public static void setManualVentilation(boolean manualVentilation){
        ManualControl.manualVentilation=manualVentilation;
    }

    public static boolean getManualLightning() {
        return ManualLightning;
    }

    public static void setManualLightning(boolean manualLightning) {
        ManualLightning = manualLightning;
    }

    public static boolean getManualWatering() {
        return ManualWatering;
    }

    public static void setManualWatering(boolean manualWatering) {
        ManualWatering = manualWatering;
    }

}
