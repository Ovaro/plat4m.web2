package ovaro.plat4m.domain;

public enum FinanceInvestmentActivityType {
    /* NEW */
    DIVDEND_REINVESTMENT(50, "DIVDEND_REINVESTMENT"),
    /* ESP */
    ESP_OFFER(17, "ESP_OFFER"),
    ESP_VEST(18, "ESP_VEST"),
    ESP_SELL(19, "ESP_SELL"),
    ESP_EXPIRE(20, "ESP_EXPIRE"),
    /* OTHERS */
    TRANSFER_SHARES_OUT(33, "TRANSFER_SHARES_OUT"),
    TRANSFER_SHARES_IN(32, "TRANSFER_SHARES_IN"),
    REINVEST_L_TERM_CG_DIST(29, "REINVEST_L_TERM_CG_DIST"),
    REINVEST_S_TERM_CG_DIST(27, "REINVEST_S_TERM_CG_DIST"),
    L_TERM_CAP_GAINS_DIST(26, "L_TERM_CAP_GAINS_DIST"),
    S_TERM_CAP_GAINS_DIST(24, "S_TERM_CAP_GAINS_DIST"),
    ADD_SHARES2(16, "ADD_SHARES"),
    REMOVE_SHARES(13, "REMOVE_SHARES"),
    ADD_SHARES(12, "ADD_SHARES"),
    REINVEST_INTEREST(10, "REINVEST_INTEREST"),
    REINVEST_DIVIDEND_COMBINED(9, "REINVEST_DIVIDEND_COMBINED"),
    RETURN_OF_CAPITAL(8, "RETURN_OF_CAPITAL"),
    INTEREST(4, "INTEREST"),
    DIVIDEND(3, "DIVIDEND"),
    SELL(2, "SELL"),
    BUY(1, "BUY"),
    INVALID(-1, "");

    public final int value;
    public final String name;

    FinanceInvestmentActivityType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public static FinanceInvestmentActivityType valueOf(int value) {
        for (FinanceInvestmentActivityType e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return INVALID;
    }
}
