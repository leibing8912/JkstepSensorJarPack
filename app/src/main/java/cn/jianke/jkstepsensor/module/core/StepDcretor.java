package cn.jianke.jkstepsensor.module.core;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.CountDownTimer;
import java.util.Timer;
import java.util.TimerTask;

public class StepDcretor implements SensorEventListener {
    final int valueNum = 5;
    float[] tempValue = new float[valueNum];
    int tempCount = 0;
    boolean isDirectionUp = false;
    int continueUpCount = 0;
    int continueUpFormerCount = 0;
    boolean lastStatus = false;
    float peakOfWave = 0;
    float valleyOfWave = 0;
    long timeOfThisPeak = 0;
    long timeOfLastPeak = 0;
    long timeOfNow = 0;
    float gravityOld = 0;
    final float initialValue = (float) 1.7;
    float ThreadValue = (float) 2.0;
    float minValue = 11f;
    float maxValue = 19.6f;
    private int CountTimeState = 0;
    public static int CURRENT_STEP = 0;
    public static int TEMP_STEP = 0;
    private int lastStep = -1;
    public static float average = 0;
    private Timer timer;
    private long duration = 2000;
    private TimeCount time;
    private OnSensorChangeListener onSensorChangeListener;

    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                calc_step(event);
            }
        }
    }

    synchronized private void calc_step(SensorEvent event) {
        average = (float) Math.sqrt(Math.pow(event.values[0], 2)
                + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
        detectorNewStep(average);
    }

    public void detectorNewStep(float values) {
        if (gravityOld == 0) {
            gravityOld = values;
        } else {
            if (DetectorPeak(values, gravityOld)) {
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = System.currentTimeMillis();

                if (timeOfNow - timeOfLastPeak >= 200
                        && (peakOfWave - valleyOfWave >= ThreadValue) && (timeOfNow - timeOfLastPeak) <= 2000) {
                    timeOfThisPeak = timeOfNow;
                    preStep();
                }
                if (timeOfNow - timeOfLastPeak >= 200
                        && (peakOfWave - valleyOfWave >= initialValue)) {
                    timeOfThisPeak = timeOfNow;
                    ThreadValue = Peak_Valley_Thread(peakOfWave - valleyOfWave);
                }
            }
        }
        gravityOld = values;
    }

    private void preStep() {
        if (CountTimeState == 0) {
            time = new TimeCount(duration, 300);
            time.start();
            CountTimeState = 1;
        } else if (CountTimeState == 1) {
            TEMP_STEP++;
        } else if (CountTimeState == 2) {
            CURRENT_STEP++;
            if (onSensorChangeListener != null) {
                onSensorChangeListener.onChange();
            }
        }
    }

    public boolean DetectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }

        if (!isDirectionUp && lastStatus
                && (continueUpFormerCount >= 2 && (oldValue >= minValue && oldValue < maxValue))) {
            peakOfWave = oldValue;
            return true;
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
            return false;
        } else {
            return false;
        }
    }

    public float Peak_Valley_Thread(float value) {
        float tempThread = ThreadValue;
        if (tempCount < valueNum) {
            tempValue[tempCount] = value;
            tempCount++;
        } else {
            tempThread = averageValue(tempValue, valueNum);
            for (int i = 1; i < valueNum; i++) {
                tempValue[i - 1] = tempValue[i];
            }
            tempValue[valueNum - 1] = value;
        }
        return tempThread;
    }

    public float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / valueNum;
        if (ave >= 8) {
            ave = (float) 4.3;
        } else if (ave >= 7 && ave < 8) {
            ave = (float) 3.3;
        } else if (ave >= 4 && ave < 7) {
            ave = (float) 2.3;
        } else if (ave >= 3 && ave < 4) {
            ave = (float) 2.0;
        } else {
            ave = (float) 1.7;
        }
        return ave;
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            time.cancel();
            CURRENT_STEP += TEMP_STEP;
            lastStep = -1;

            timer = new Timer(true);
            TimerTask task = new TimerTask() {
                public void run() {
                    if (lastStep == CURRENT_STEP) {
                        timer.cancel();
                        CountTimeState = 0;
                        lastStep = -1;
                        TEMP_STEP = 0;
                    } else {
                        lastStep = CURRENT_STEP;
                    }
                }
            };
            timer.schedule(task, 0, 1000);
            CountTimeState = 2;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (lastStep == TEMP_STEP) {
                time.cancel();
                CountTimeState = 0;
                lastStep = -1;
                TEMP_STEP = 0;
            } else {
                lastStep = TEMP_STEP;
            }
        }
    }

    public void setOnSensorChangeListener(OnSensorChangeListener onSensorChangeListener) {
        this.onSensorChangeListener = onSensorChangeListener;
    }

    public interface OnSensorChangeListener {
        void onChange();
    }
}
