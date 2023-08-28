package it.unipi.aide.iot.bean;
import java.sql.Timestamp;


public class HumiditySample {
    private int node;
    private int humidity;
    private Timestamp timestamp;

    public HumiditySample(int node, int humidity, Timestamp timestamp) {
        this.node = node;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }
    public int getNode() {
        return node;
    }
    public void setNode(int node) {
        this.node = node;
    }

    public int getHumidity() {
        return humidity;
    }
    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }
    public Timestamp getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

}
