package cn.jianke.jkstepsensor.module.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import cn.jianke.customcache.utils.StringUtil;
import cn.jianke.jkstepsensor.common.Constant;
import cn.jianke.jkstepsensor.common.data.DataCache;
import cn.jianke.jkstepsensor.common.data.bean.StepModel;
import cn.jianke.jkstepsensor.common.utils.DateUtils;
import cn.jianke.jkstepsensor.common.utils.NotificationUtils;
import cn.jianke.jkstepsensor.module.core.StepDcretor;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class StepService extends Service implements SensorEventListener {
    public static final int INT_ERROR = -12;
    public static final String ACTION_STOP_SERVICE = "action_stop_service";
    public final static String STEP_KEY = "step_key";
    private SensorManager sensorManager;
    private StepDcretor stepDetector;
    private MsgHandler msgHandler = new MsgHandler();
    private Messenger messenger = new Messenger(msgHandler);
    private StepModel mStepModel;
    private BroadcastReceiver stepServiceReceiver;
    private boolean isNeedStopService = false;

    class MsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.MSG_FROM_CLIENT:
                    try {
                        cacheStepData(StepService.this, StepDcretor.CURRENT_STEP + "");
                        updateNotification(msg.getData());
                        Messenger messenger = msg.replyTo;
                        Message replyMsg = Message.obtain(null, Constant.MSG_FROM_SERVER);
                        Bundle bundle = new Bundle();
                        bundle.putInt(STEP_KEY, StepDcretor.CURRENT_STEP);
                        replyMsg.setData(bundle);
                        messenger.send(replyMsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void updateNotification(Bundle bundle) {
        if (bundle == null) {
            NotificationUtils.getInstance(StepService.this).
                    updateNotification("today walk " + StepDcretor.CURRENT_STEP + " step");
        }else {
            String content = (String) bundle.getSerializable(Constant.CONTENT_KEY);
            String ticker = (String) bundle.getSerializable(Constant.TICKER_KEY);
            String contentTile = (String) bundle.getSerializable(Constant.CONTENTTITLE_KEY);
            Class pendingClass = (Class) bundle.getSerializable(Constant.PENDINGCLASS_KEY);
            boolean isOngoing = true;
            if (bundle.getSerializable(Constant.ISONGOING_KEY) != null){
                isOngoing = (boolean) bundle.getSerializable(Constant.ISONGOING_KEY);
            }
            int icon = INT_ERROR;
            if (bundle.getSerializable(Constant.ICON_KEY) != null){
                icon = (int) bundle.getSerializable(Constant.ICON_KEY);
            }
            int notifyId = INT_ERROR;
            if (bundle.getSerializable(Constant.NOTIFYID_KEY) != null){
                notifyId = (int) bundle.getSerializable(Constant.NOTIFYID_KEY);
            }
            if (StringUtil.isEmpty(content)
                    || StringUtil.isEmpty(ticker)
                    || StringUtil.isEmpty(contentTile)){
                NotificationUtils.getInstance(StepService.this).
                        updateNotification("today walk " + StepDcretor.CURRENT_STEP + " step");
            }else {
                NotificationUtils.getInstance(StepService.this).
                        updateNotification(content + StepDcretor.CURRENT_STEP + " step",
                                ticker,
                                contentTile,
                                StepService.this,
                                pendingClass,
                                isOngoing,
                                notifyId,
                                icon);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initStepServiceReceiver();
        startStep();
    }

    private void initStepServiceReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STOP_SERVICE);
        stepServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_STOP_SERVICE.equals(action)){
                    isNeedStopService = true;
                    StepService.this.stopSelf();
                }
            }
        };
        registerReceiver(stepServiceReceiver, filter);
    }

    private void startStep() {
        startStepDetector();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void cacheStepData(Context context, String stepCount){
        mStepModel = new StepModel();
        mStepModel.setDate(DateUtils.simpleDateFormat(new Date()));
        mStepModel.setStep(stepCount);
        DataCache.getInstance().addStepCache(context, mStepModel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    private void startStepDetector() {
        if (sensorManager != null && stepDetector != null) {
            sensorManager.unregisterListener(stepDetector);
            sensorManager = null;
            stepDetector = null;
        }
        DataCache.getInstance().getTodayCache(this, new DataCache.DataCacheListener() {
            @Override
            public void readListCache(StepModel stepModel) {
                if (stepModel != null){
                   StepDcretor.CURRENT_STEP = Integer.parseInt(stepModel.getStep());
                }
            }

            @Override
            public void readAllCache(ArrayList<StepModel> mData) {

            }
        });

        sensorManager = (SensorManager) this
                .getSystemService(SENSOR_SERVICE);
        addBasePedoListener();
        addCountStepListener();
    }

    public void stopStepDetector(){
        if (sensorManager != null && stepDetector != null) {
            sensorManager.unregisterListener(stepDetector);
            sensorManager = null;
            stepDetector = null;
        }
    }

    private void addCountStepListener() {
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (detectorSensor != null) {
            sensorManager.registerListener(StepService.this, detectorSensor, SensorManager.SENSOR_DELAY_UI);
        } else if (countSensor != null) {
            sensorManager.registerListener(StepService.this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
        }
    }

    private void addBasePedoListener() {
        stepDetector = new StepDcretor();
        Sensor sensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(stepDetector, sensor,
                SensorManager.SENSOR_DELAY_UI);
        stepDetector
                .setOnSensorChangeListener(new StepDcretor.OnSensorChangeListener() {

                    @Override
                    public void onChange() {
                    }
                });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        unregisterReceiver(stepServiceReceiver);
        stopStepDetector();
        if (!isNeedStopService){
            Intent intent = new Intent(this, StepService.class);
            startService(intent);
        }else {
            isNeedStopService = false;
        }
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}
