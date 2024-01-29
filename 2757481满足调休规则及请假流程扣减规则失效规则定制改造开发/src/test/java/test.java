import org.junit.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

public class test {

    /**
     * 获取指定日期的季度范围时间
     * @param fromDate
     * @return
     */
    @Test
    public List<String> getQuarters(String fromDate){
        fromDate="2024-01-01";
        ArrayList<String> quarterDates = new ArrayList<>();
        LocalDate currentDate = LocalDate.parse(fromDate);
        int currentYear = currentDate.getYear();
        int currentQuarter = currentDate.get(IsoFields.QUARTER_OF_YEAR);

        LocalDate startOfQuarter = LocalDate.of(currentYear, getQuarterStartMonth(currentQuarter), 1);
        LocalDate endOfQuarter = startOfQuarter.plusMonths(2).withDayOfMonth(startOfQuarter.lengthOfMonth());

        quarterDates.add(startOfQuarter.format(DateTimeFormatter.ISO_DATE));
        quarterDates.add(endOfQuarter.format(DateTimeFormatter.ISO_DATE));

        return quarterDates;
    }
    private static Month getQuarterStartMonth(int quarter) {
        switch (quarter) {
            case 1:
                return Month.JANUARY;
            case 2:
                return Month.APRIL;
            case 3:
                return Month.JULY;
            case 4:
                return Month.OCTOBER;
            default:
                throw new IllegalArgumentException("Invalid quarter: " + quarter);
        }
    }
}
