package it.unipi.aide.iot.bean;

import java.sql.Timestamp;

public class TemperatureSample {
    private int nodeId;
    private int temperature;
    private Timestamp timestamp;

    public TemperatureSample(int idNode, int temperature, Timestamp timestamp) {
        this.nodeId = idNode;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

}
