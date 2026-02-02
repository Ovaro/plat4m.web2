package ovaro.plat4m.service.idto;

public class LatticeCompanyProfileIDTO {

    private String zip; // 3008
    private String sector; // Financial Services
    private String fullTimeEmployees; //": 39529,
    private String longBusinessSummary; //": "Australia and New Zealand Banking Group Limited provides various banking and financial products and services in Australia and internationally. Its Australia Retail and Commercial division offers various products and services to consumer customers through the branch network, mortgage specialists, contact centers, self-service channels, and third-party brokers, as well as financial planning services. It also provides asset financing for medium to large commercial customers, agribusiness customers, small business owners, high net worth individuals, and family groups. The company's Institutional division offers documentary trade, supply chain and commodity financing, cash management solutions, deposits, payments, and clearing services; loan syndication, loan structuring and execution, project and export finance, debt structuring and acquisition finance, and corporate advisory services, as well as loan products; and risk management services. It serves governments, and global institutional and corporate customers. The company's New Zealand division provides banking and wealth management services to consumer, and private banking and small business banking customers through its Internet and app-based digital solutions, network of branches, mortgage specialists, relationship managers, and contact centers; and traditional relationship banking and financial solutions for medium to large enterprises, agricultural business segments, and government and government-related entities. Its Pacific division offers retail products, and traditional relationship banking and financial solutions. This division serves retail customers, small to medium-sized enterprises, institutional customers, and governments. Australia and New Zealand Banking Group Limited has a strategic partnership with Cashrewards Limited to launch Cashrewards MaxTM for Australia and New Zealand consumer credit and debit card holders. The company was founded in 1835 and is headquartered in Melbourne, Australia.",
    private String city; //": "Melbourne",
    private String phone; //": "61 3 9273 5555",
    private String state; //": "VIC",
    private String country; //": "Australia",

    private String website; //": "https://www.anz.com.au",
    private Integer maxAge; //": 86400,
    private String address1; //": "ANZ Centre Melbourne"
    private String industry; //": "Banks\u2014Diversified",
    private String address2; //": "Level 9 833 Collins Street Docklands"

    public LatticeCompanyProfileIDTO() {}

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getFullTimeEmployees() {
        return fullTimeEmployees;
    }

    public void setFullTimeEmployees(String fullTimeEmployees) {
        this.fullTimeEmployees = fullTimeEmployees;
    }

    public String getLongBusinessSummary() {
        return longBusinessSummary;
    }

    public void setLongBusinessSummary(String longBusinessSummary) {
        this.longBusinessSummary = longBusinessSummary;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }
}
