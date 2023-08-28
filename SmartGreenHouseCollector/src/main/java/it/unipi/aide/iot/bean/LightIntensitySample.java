package it.unipi.aide.iot.bean;

import java.sql.Timestamp;

public class LightIntensitySample {
    private int node;
    private int intensity;
    private Timestamp timestamp;

    public LightIntensitySample(int node, int intensity, Timestamp timestamp) {
        this.node = node;
        this.intensity = intensity;
        this.timestamp = timestamp;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
