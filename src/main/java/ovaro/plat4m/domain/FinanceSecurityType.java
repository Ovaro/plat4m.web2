package ovaro.plat4m.domain;

public enum FinanceSecurityType {
    STOCK(1),
    MUTUAL_FUND(2),
    SUPERANNUATION(4),
    INDEX(7),
    EMPLOYEE_STOCK_OPTION(8),
    CASH(9),
    OTHER(10),
    UNKNOWN(-1),
    ETF(11);

    private final int mnyType;

    FinanceSecurityType(int mnyType) {
        this.mnyType = mnyType;
    }

    public static FinanceSecurityType toSecurityType(int mnyType) {
        switch (mnyType) {
            case 1:
                return STOCK;
            case 2:
                return MUTUAL_FUND;
            case 4:
                return SUPERANNUATION;
            case 7:
                return INDEX;
            case 8:
                return EMPLOYEE_STOCK_OPTION;
            case 9:
                return CASH;
            case 10:
                return OTHER;
            case 11:
                return ETF;
            default:
                return UNKNOWN;
        }
    }

    public int value() {
        return mnyType;
    }
    // public static String getValueString(int mnyType) {
    //     switch (mnyType) {
    //     case 0:
    //         return "stock";
    //     case 1:
    //         return "mutual-fund";
    //     case 4:
    //         return "superannuation";
    //     case 7:
    //         return "index";
    //     case 8:
    //         return "employee-stock-option";
    //     case 9:
    //         return "cash";
    //     case 10:
    //         return "other-investment";
    //     case 11:
    //         return "etf";
    //     default:
    //         return "unknown-investment";
    //     }
    // }

}
