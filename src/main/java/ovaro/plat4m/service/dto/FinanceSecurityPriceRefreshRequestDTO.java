package ovaro.plat4m.service.dto;

public class FinanceSecurityPriceRefreshRequestDTO {

    private String userSecurityId;
    private String accountId;

    public String getUserSecurityId() {
        return userSecurityId;
    }

    public void setUserSecurityId(String userSecurityId) {
        this.userSecurityId = userSecurityId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
