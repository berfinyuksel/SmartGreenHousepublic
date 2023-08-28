package it.unipi.aide.iot.mqtt.sensors;

import it.unipi.aide.iot.bean.TemperatureSample;
import it.unipi.aide.iot.persistence.DBDriver;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TemperatureDevice {
    public String TEMPERATURE_TOPIC = "temperature";
    private List<TemperatureSample> lastSamples;
    private float lowerBound;
    private float upperBound;


    public TemperatureDevice(){
        lastSamples= new ArrayList<>();
        lowerBound=15;
        upperBound=25;
    }

    public float getCurrentTemperature(){
        return lastSamples.get(lastSamples.size()-1).getTemperature();
    }

    public void addSample(TemperatureSample temperatureSample){
        temperatureSample.setTimestamp(new Timestamp(System.currentTimeMillis()));
        lastSamples.add(temperatureSample);

        Iterator<TemperatureSample> iterator = lastSamples.iterator();

        while (iterator.hasNext()) {
            TemperatureSample obj = iterator.next();
            long secondsDifference = java.time.Duration.between(obj.getTimestamp().toInstant(), java.time.Instant.now()).getSeconds();
            if (secondsDifference >= 30) {
                iterator.remove();
            }
        }  // remove old samples from the lastSamples list, if it has been done in the last 30sec
        DBDriver.getInstance().insertTemperatureSample(temperatureSample);
    }

    public float getAvgTemperature(){
        return (float) lastSamples.stream()
                .mapToDouble(TemperatureSample::getTemperature)
                .average()
                .orElse(0.0); // provide a default value if the list is empty
    }

    public float getLowerBound(){
        return lowerBound;
    }

    public void setLowerBound(float value){
        this.lowerBound=value;
    }

    public float getUpperBound(){
        return upperBound;
    }

    public void setUpperBound(float value){
        this.upperBound=value;
    }

}
