package it.unipi.aide.iot.mqtt.sensors;
import java.sql.Timestamp;


import it.unipi.aide.iot.bean.LightIntensitySample;
import it.unipi.aide.iot.persistence.DBDriver;

public class LightIntensityDevice {
    public String LIGHTINTENSITY_TOPIC = "light";
    private int lastSample;
    private float lowerBound;
    private float upperBound;


    public LightIntensityDevice() {
        lowerBound=10000;
        upperBound=15000;
    }

      public int getCurrentIntensityLevel(){
        return lastSample;
    }

    public void addSample(LightIntensitySample lightintensitysample){
        this.lastSample=lightintensitysample.getIntensity();
        lightintensitysample.setTimestamp(new Timestamp(System.currentTimeMillis()));
        DBDriver.getInstance().insertLightIntensitySample(lightintensitysample);
    }

    public float getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(float lowerBound) {
        this.lowerBound = lowerBound;
    }

    public float getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(float upperBound) {
        this.upperBound = upperBound;
    }

   } 
