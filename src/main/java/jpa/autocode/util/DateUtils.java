package jpa.autocode.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    public static String formateDate(String formate) {
        return formateDate(formate, new Date());
    }

    public static String formateDate(String formate, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(formate);
        return sdf.format(date == null ? new Date() : date);
    }
}
