package ovaro.plat4m.service.idto;

import ovaro.plat4m.domain.User;

/**
 * A DTO representing a user, with only the public attributes.
 */
public class TwelveDataStocksIDTO {

    private TwelveDataStockDataIDTO[] data;

    public TwelveDataStocksIDTO() {
        // Empty constructor needed for Jackson.
    }

    public TwelveDataStockDataIDTO[] getData() {
        return data;
    }

    public void setData(TwelveDataStockDataIDTO[] data) {
        this.data = data;
    }
}
