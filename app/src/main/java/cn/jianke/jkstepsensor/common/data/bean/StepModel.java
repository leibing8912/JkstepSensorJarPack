package cn.jianke.jkstepsensor.common.data.bean;

import java.io.Serializable;

public class StepModel implements Serializable{
    private static final long serialVersionUID = 1803672514800467436L;
    private String date;
    private String step;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }
}
