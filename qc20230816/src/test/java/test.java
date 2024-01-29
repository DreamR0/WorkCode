import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class test {

    /**
     * 有序且升序的整数型数组，目标值为target,找到就输出下标，没找到就输出-1，二分查找
     */
    @Test
    public void test(){
       String resourceids = "1,2,3,4,5,6,7,8,9,10,11,12,13";
       String resourceid1 = resourceids.substring(0,resourceids.length()/2);
       String resourceid2 = resourceids.substring(resourceids.length()/2);
        System.out.println(resourceid1);
        System.out.println(resourceid2);
    }

    @Test
    public void test1(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        //获取值班开始日期和结束日期
        LocalDate startDate = LocalDate.parse("2023-11-24");
        LocalDate endDate = LocalDate.parse("2023-11-27");
        List<LocalDate> dateRange = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dateRange.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        for (LocalDate date : dateRange){
            String kqDate = date.format(formatter);
            System.out.println(kqDate);
        }

    }
}
