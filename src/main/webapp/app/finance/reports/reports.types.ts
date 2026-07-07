export type ReportRowDimension = 'categoryType' | 'category' | 'subcategory';
export type ReportColumnDimension = 'halfMonth' | 'month' | 'quarter' | 'year';
export type ReportView = 'report' | 'bar' | 'line' | 'pie';
export type ReportDatePreset = '1M' | '3M' | '6M' | '12M' | '2Y' | '5Y' | '7Y' | '10Y' | 'YTD' | 'ALL' | 'custom';

export interface ReportConfig {
  id?: string | null;
  reportKey: string;
  name: string;
  title: string;
  rowDimension: ReportRowDimension;
  columnDimension: ReportColumnDimension;
  showPercentOfTotal: boolean;
  defaultView: ReportView;
  datePreset: ReportDatePreset;
  startDate: string | null;
  endDate: string | null;
  builtin: boolean;
  editable: boolean;
  accountIds: string[];
  categoryIds: string[];
  payeeIds: string[];
  familyMemberIds: string[];
}

export interface ReportDefinition {
  key: string;
  title: string;
  description: string;
  defaultConfig: ReportConfig;
}

export interface ReportResultColumn {
  key: string;
  label: string;
  startDate: string;
  endDate: string;
  total: number;
}

export interface ReportResultRow {
  key: string;
  label: string;
  groupLabel: string | null;
  parentLabel: string | null;
  subtotal: boolean;
  grandTotal: boolean;
  rowType: string;
  total: number;
  percentOfTotal: number;
  values: number[];
  valuePercents: number[];
}

export interface ReportResultSeries {
  name: string;
  data: number[];
}

export interface ReportPieSegment {
  label: string;
  value: number;
  percentOfTotal: number;
}

export interface ReportResult {
  title: string;
  currencyCode: string;
  rowDimension: ReportRowDimension;
  columnDimension: ReportColumnDimension;
  defaultView: ReportView;
  showPercentOfTotal: boolean;
  startDate: string;
  endDate: string;
  grandTotal: number;
  columns: ReportResultColumn[];
  rows: ReportResultRow[];
  series: ReportResultSeries[];
  pie: ReportPieSegment[];
}

export interface ReportDrilldownRequest {
  config: ReportConfig;
  rowKey: string;
  rowLabel: string;
  columnKey: string | null;
  columnLabel: string | null;
}

export interface ReportDrilldownTransaction {
  id: string;
  date: string;
  accountId: string | null;
  payeeName: string | null;
  categoryName: string | null;
  familyMemberName: string | null;
  memo: string | null;
  sectionLabel: string | null;
  amount: number;
  originalCurrencyCode: string | null;
  originalAmount: number | null;
}

export interface ReportDrilldownResult {
  title: string;
  rowKey: string;
  rowLabel: string;
  columnKey: string | null;
  columnLabel: string | null;
  currencyCode: string;
  total: number;
  transactions: ReportDrilldownTransaction[];
}

export interface ReportFilterOption {
  id: string;
  label: string;
  parentId?: string | null;
  rootType?: 'Income' | 'Expense' | null;
}

export interface ReportTreeOption {
  key: string;
  label: string;
  selectable: boolean;
  leaf: boolean;
  children: ReportTreeOption[];
}
