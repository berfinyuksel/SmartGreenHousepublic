package it.unipi.aide.iot.mqtt.sensors;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unipi.aide.iot.bean.HumiditySample;
import it.unipi.aide.iot.persistence.DBDriver;

public class HumidityDevice {

    public String HUMIDITY_TOPIC = "humidity";
    private List<HumiditySample> lastSamples;
    private float lowerBound;
    private float upperBound;


    public HumidityDevice() {
        lastSamples= new ArrayList<>();
        lowerBound=30;
        upperBound=60; 
    }

      public float getCurrentHumidityLevel(){
        return lastSamples.get(lastSamples.size()-1).getHumidity();
    }

    public void addSample(HumiditySample humiditysample){
        humiditysample.setTimestamp(new Timestamp(System.currentTimeMillis()));
        lastSamples.add(humiditysample);
        Iterator<HumiditySample> iterator = lastSamples.iterator();

        while (iterator.hasNext()) {
            HumiditySample obj = iterator.next();
            long secondsDifference = java.time.Duration.between(obj.getTimestamp().toInstant(), java.time.Instant.now()).getSeconds();
            if (secondsDifference >= 30) {
                iterator.remove();
            }
        }
        DBDriver.getInstance().insertHumiditySample(humiditysample);

    }

    public float getAvgHumidity(){
        return (float) lastSamples.stream()
                .mapToDouble(HumiditySample::getHumidity)
                .average()
                .orElse(0.0);

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

