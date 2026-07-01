package ovaro.plat4m.service.dto;

import java.math.BigDecimal;

public class FinanceTransactionImportDraftRequestDTO {

    private String rawContent;
    private String rawHtml;
    private BigDecimal expectedEndingBalance;

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getRawHtml() {
        return rawHtml;
    }

    public void setRawHtml(String rawHtml) {
        this.rawHtml = rawHtml;
    }

    public BigDecimal getExpectedEndingBalance() {
        return expectedEndingBalance;
    }

    public void setExpectedEndingBalance(BigDecimal expectedEndingBalance) {
        this.expectedEndingBalance = expectedEndingBalance;
    }
}
