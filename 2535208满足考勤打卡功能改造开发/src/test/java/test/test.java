package test;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class test {
    @Test
    public  void dateToWeek() {
        String shiftdate = "2023-10-15";
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance();
            c.setTime(format.parse(shiftdate));
            int dayForWeek = 0;
            if(c.get(Calendar.DAY_OF_WEEK) == 1){
                dayForWeek = 7;
            }else{
                dayForWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
            }
            System.out.println(dayForWeek);
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
