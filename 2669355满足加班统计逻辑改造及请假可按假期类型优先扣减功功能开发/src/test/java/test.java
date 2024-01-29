import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class test {

    @Test
    public void test1(){
       String fromDate = "2023-11-01";
       String DateMonth = fromDate.substring(0,7);
        System.out.println(DateMonth);
        System.out.println(fromDate.length());
    }
}
