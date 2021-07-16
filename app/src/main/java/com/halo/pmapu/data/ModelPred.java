package com.halo.pmapu.data;

/**
 * Created by karsk on 08/01/2018.
 */

public class ModelPred {

    String value, time;

    public ModelPred(String value, String time) {
        this.value = value;
        this.time = time;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
