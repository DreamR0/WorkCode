import org.junit.Test;
import weaver.general.Util;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class test {

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
