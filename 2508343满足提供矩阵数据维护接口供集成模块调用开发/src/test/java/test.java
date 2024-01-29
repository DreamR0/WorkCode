import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;

public class test {

    @Test
    public void test1(){
        String monthString = "2024-01";

        YearMonth yearMonth = YearMonth.parse(monthString);

        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

        System.out.println("First day of the month: " + firstDayOfMonth);
        System.out.println("Last day of the month: " + lastDayOfMonth);
    }
}
