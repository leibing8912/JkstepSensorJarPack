package cn.jianke.jkstepsensor.common.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import cn.jianke.customcache.utils.StringUtil;
import cn.jianke.jkstepsensor.R;
import cn.jianke.jkstepsensor.module.service.StepService;

public class NotificationUtils {
    private static NotificationUtils intance;
    private NotificationManager nm;
    private NotificationCompat.Builder builder;

    private NotificationUtils(Context context){
        builder = new NotificationCompat.Builder(context);
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    public static NotificationUtils getInstance(Context context){
        if (intance == null){
            intance = new NotificationUtils(context);
        }
        return intance;
    }

    public void updateNotification(
                                    String content, String ticker ,String contentTitle,
                                   Context context, Class pendingClass,
                                   boolean isOngoing, int notifyId,
                                   int icon){
        if (builder == null || nm == null)
            return;
        builder.setPriority(Notification.PRIORITY_MIN);
        if (content != null && pendingClass != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, pendingClass), 0);
            builder.setContentIntent(contentIntent);
        }
        if (StringUtil.isEmpty(ticker))
            ticker = "jianke step";
            builder.setTicker(ticker);
        if (icon == StepService.INT_ERROR)
            icon = R.mipmap.ic_launcher;
        builder.setSmallIcon(icon);
        if (StringUtil.isEmpty(contentTitle))
            contentTitle = "jianke step";
        builder.setContentTitle(contentTitle);
        builder.setOngoing(isOngoing);
        if (StringUtil.isNotEmpty(content))
            builder.setContentText(content);
        Notification notification = builder.build();
        if (notifyId == StepService.INT_ERROR)
            notifyId = R.string.app_name;
        nm.notify(notifyId, notification);
    }

    public void updateNotification(
            String content, String ticker ,String contentTitle,
            Context context, Class pendingClass){
        updateNotification(content, ticker, contentTitle,
                context, pendingClass,true,
                R.string.app_name, R.mipmap.ic_launcher);
    }

    public void updateNotification(
            String content, String ticker ,String contentTitle){
            updateNotification(content,ticker,contentTitle,
                    null,null,true,
                    R.string.app_name, R.mipmap.ic_launcher);
    }

    public void updateNotification(
            String content, String ticker ,String contentTitle,int icon){
        updateNotification(content,ticker,contentTitle,
                null,null,true,
                R.string.app_name,icon);
    }

    public void updateNotification(
            String content){
        updateNotification(content,null,null,
                null,null,true,
                R.string.app_name, R.mipmap.icon);
    }

    public void clearAllNotification(){
        if (nm != null){
            nm.cancelAll();
        }
    }

    public void clearNotificationById(int id){
        if (nm != null){
            nm.cancel(id);
        }
    }
}
