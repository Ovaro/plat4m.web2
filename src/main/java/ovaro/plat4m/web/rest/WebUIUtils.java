package ovaro.plat4m.web.rest;

import java.time.LocalDate;

public class WebUIUtils {

    public static LocalDate getDateFromPeriod(String period) {
        switch (period) {
            case "1D":
                return LocalDate.now().minusDays(1);
            case "1W":
                return LocalDate.now().minusDays(7);
            case "1M":
                return LocalDate.now().minusMonths(1);
            case "1Q":
                return LocalDate.now().minusMonths(3);
            case "6M":
                return LocalDate.now().minusMonths(6);
            case "1Y":
                return LocalDate.now().minusYears(1);
            case "2Y":
                return LocalDate.now().minusYears(2);
            case "3Y":
                return LocalDate.now().minusYears(3);
            case "4Y":
                return LocalDate.now().minusYears(4);
            case "5Y":
                return LocalDate.now().minusYears(5);
            case "10Y":
                return LocalDate.now().minusYears(10);
            default:
                return LocalDate.now();
        }
    }
}
