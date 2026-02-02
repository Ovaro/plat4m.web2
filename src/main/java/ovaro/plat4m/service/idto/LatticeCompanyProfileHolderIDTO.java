package ovaro.plat4m.service.idto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LatticeCompanyProfileHolderIDTO {

    private String status;

    @JsonProperty("company_profile")
    private LatticeCompanyProfileIDTO companyProfile;

    public LatticeCompanyProfileHolderIDTO() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LatticeCompanyProfileIDTO getCompanyProfile() {
        return companyProfile;
    }

    public void setCompanyProfile(LatticeCompanyProfileIDTO companyProfile) {
        this.companyProfile = companyProfile;
    }
}
