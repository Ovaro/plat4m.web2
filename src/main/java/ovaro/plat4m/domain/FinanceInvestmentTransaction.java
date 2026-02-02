package ovaro.plat4m.domain;

import java.io.Serializable;

// @Entity
// @Table(name = "fin_inv_transaction")
// @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FinanceInvestmentTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    // @Id
    // @GeneratedValue(strategy = GenerationType.AUTO)
    // private UUID id;

    private Double price;

    private Double quantity;

    // public UUID getId() {
    //     return id;
    // }

    // public void setId(UUID id) {
    //     this.id = id;
    // }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
}
