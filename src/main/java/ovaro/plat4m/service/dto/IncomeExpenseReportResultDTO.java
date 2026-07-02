package ovaro.plat4m.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IncomeExpenseReportResultDTO {

    private String title;
    private String currencyCode;
    private String rowDimension;
    private String columnDimension;
    private String defaultView;
    private boolean showPercentOfTotal;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal grandTotal = BigDecimal.ZERO;
    private List<ColumnDTO> columns = new ArrayList<>();
    private List<RowDTO> rows = new ArrayList<>();
    private List<SeriesDTO> series = new ArrayList<>();
    private List<PieSegmentDTO> pie = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getRowDimension() {
        return rowDimension;
    }

    public void setRowDimension(String rowDimension) {
        this.rowDimension = rowDimension;
    }

    public String getColumnDimension() {
        return columnDimension;
    }

    public void setColumnDimension(String columnDimension) {
        this.columnDimension = columnDimension;
    }

    public String getDefaultView() {
        return defaultView;
    }

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }

    public boolean isShowPercentOfTotal() {
        return showPercentOfTotal;
    }

    public void setShowPercentOfTotal(boolean showPercentOfTotal) {
        this.showPercentOfTotal = showPercentOfTotal;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public List<ColumnDTO> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnDTO> columns) {
        this.columns = columns;
    }

    public List<RowDTO> getRows() {
        return rows;
    }

    public void setRows(List<RowDTO> rows) {
        this.rows = rows;
    }

    public List<SeriesDTO> getSeries() {
        return series;
    }

    public void setSeries(List<SeriesDTO> series) {
        this.series = series;
    }

    public List<PieSegmentDTO> getPie() {
        return pie;
    }

    public void setPie(List<PieSegmentDTO> pie) {
        this.pie = pie;
    }

    public static class ColumnDTO {

        private String key;
        private String label;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal total = BigDecimal.ZERO;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }
    }

    public static class RowDTO {

        private String key;
        private String label;
        private String groupLabel;
        private String parentLabel;
        private boolean subtotal;
        private boolean grandTotal;
        private String rowType;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal percentOfTotal = BigDecimal.ZERO;
        private List<BigDecimal> values = new ArrayList<>();
        private List<BigDecimal> valuePercents = new ArrayList<>();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getGroupLabel() {
            return groupLabel;
        }

        public void setGroupLabel(String groupLabel) {
            this.groupLabel = groupLabel;
        }

        public String getParentLabel() {
            return parentLabel;
        }

        public void setParentLabel(String parentLabel) {
            this.parentLabel = parentLabel;
        }

        public boolean isSubtotal() {
            return subtotal;
        }

        public void setSubtotal(boolean subtotal) {
            this.subtotal = subtotal;
        }

        public boolean isGrandTotal() {
            return grandTotal;
        }

        public void setGrandTotal(boolean grandTotal) {
            this.grandTotal = grandTotal;
        }

        public String getRowType() {
            return rowType;
        }

        public void setRowType(String rowType) {
            this.rowType = rowType;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }

        public BigDecimal getPercentOfTotal() {
            return percentOfTotal;
        }

        public void setPercentOfTotal(BigDecimal percentOfTotal) {
            this.percentOfTotal = percentOfTotal;
        }

        public List<BigDecimal> getValues() {
            return values;
        }

        public void setValues(List<BigDecimal> values) {
            this.values = values;
        }

        public List<BigDecimal> getValuePercents() {
            return valuePercents;
        }

        public void setValuePercents(List<BigDecimal> valuePercents) {
            this.valuePercents = valuePercents;
        }
    }

    public static class SeriesDTO {

        private String name;
        private List<BigDecimal> data = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<BigDecimal> getData() {
            return data;
        }

        public void setData(List<BigDecimal> data) {
            this.data = data;
        }
    }

    public static class PieSegmentDTO {

        private String label;
        private BigDecimal value = BigDecimal.ZERO;
        private BigDecimal percentOfTotal = BigDecimal.ZERO;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public BigDecimal getPercentOfTotal() {
            return percentOfTotal;
        }

        public void setPercentOfTotal(BigDecimal percentOfTotal) {
            this.percentOfTotal = percentOfTotal;
        }
    }
}
