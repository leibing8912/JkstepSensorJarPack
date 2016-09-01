package cn.jianke.jkstepsensor.common.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static String simpleDateFormat(Date date){
        SimpleDateFormat dateFm = new SimpleDateFormat("yyyy/MM/dd");
        String dateTime = dateFm.format(date).trim().toString();
        return dateTime;
    }

    public static Date getNextDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        date = calendar.getTime();
        return date;
    }
}
