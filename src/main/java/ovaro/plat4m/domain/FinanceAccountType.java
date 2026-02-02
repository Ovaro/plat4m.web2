package ovaro.plat4m.domain;

public enum FinanceAccountType {
    BANKING(0),
    CREDIT_CARD(1),
    CASH(2),
    ASSET(3),
    LIABILITY(4),
    INVESTMENT(5),
    LOAN(6),
    UNKNOWN(-1);

    private final int mnyType;

    FinanceAccountType(int mnyType) {
        this.mnyType = mnyType;
    }

    public static FinanceAccountType toAccountType(int mnyType) {
        switch (mnyType) {
            case 0:
                return BANKING;
            case 1:
                return CREDIT_CARD;
            case 2:
                return CASH;
            case 3:
                return ASSET;
            case 4:
                return LIABILITY;
            case 5:
                return INVESTMENT;
            case 6:
                return LOAN;
            default:
                return UNKNOWN;
        }
    }

    public int getValue() {
        return mnyType;
    }

    public String getValueString() {
        return getValueString(this.mnyType);
    }

    public static String getValueString(int mnyType) {
        switch (mnyType) {
            case 0:
                return "bank";
            case 1:
                return "credit";
            case 2:
                return "cash";
            case 3:
                return "asset";
            case 4:
                return "liability";
            case 5:
                return "investment";
            case 6:
                return "loan";
            default:
                return "unknown";
        }
    }

    public static int fromValueString(String stringType) {
        switch (stringType) {
            case "bank":
                return 0;
            case "credit":
                return 1;
            case "cash":
                return 2;
            case "asset":
                return 3;
            case "liability":
                return 4;
            case "investment":
                return 5;
            case "loan":
                return 6;
            default:
                return 0;
        }
    }
}
