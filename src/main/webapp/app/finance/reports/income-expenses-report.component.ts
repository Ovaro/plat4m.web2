import { ChangeDetectorRef, Component, DEFAULT_CURRENCY_CODE, Inject, LOCALE_ID, OnInit, inject } from '@angular/core';
import { CookieService } from 'ngx-cookie';
import { DialogModule } from 'primeng/dialog';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexLegend,
  ApexMarkers,
  ApexPlotOptions,
  ApexStroke,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  NgApexchartsModule,
} from 'ng-apexcharts';
import { forkJoin } from 'rxjs';

import SharedModule from 'app/shared/shared.module';
import { AccountList } from '../account-list/account-list.service';
import { Transactions } from '../transactions/transactions.service';
import { FinanceManageDataService } from '../manage-data/finance-manage-data.service';
import { ManagedCategory, ManagedPayee } from '../manage-data/finance-manage-data.types';
import { FinancialAccount } from '../finance.model';
import { formatCurrencyAmount } from '../transaction-history-dialog.utils';
import { TransactionTreeOption } from '../transactions/transactions.types';
import {
  ReportConfig,
  ReportDatePreset,
  ReportDefinition,
  ReportDrilldownTransaction,
  ReportDrilldownResult,
  ReportFilterOption,
  ReportResultColumn,
  ReportResultRow,
  ReportResult,
  ReportRowDimension,
  ReportTreeOption,
  ReportView,
} from './reports.types';
import { ReportsService } from './reports.service';

type ViewOption = { label: string; value: ReportView };
type DateOption = { label: string; value: ReportDatePreset };
type SectionFilterOption = { label: string; value: SectionFilter };
type ChartSectionFilter = Exclude<SectionFilter, 'all'>;
type ChartSectionFilterOption = { label: string; value: ChartSectionFilter };
type ConfigTab = 'layout' | 'accounts' | 'categories' | 'payees' | 'family' | 'title' | 'view';
type FilterListKey = 'accountIds' | 'categoryIds' | 'payeeIds' | 'familyMemberIds';
type SectionFilter = 'all' | 'income' | 'expense';
type ChartAggregateRow = {
  label: string;
  section: string;
  total: number;
  values: number[];
};
type PersistedIdList = {
  mode: 'allExcept' | 'only';
  ids: string[];
};
type PersistedWorkingConfig = Omit<Partial<ReportConfig>, 'accountIds' | 'categoryIds' | 'payeeIds' | 'familyMemberIds'> & {
  accountIds?: PersistedIdList;
  categoryIds?: PersistedIdList;
  payeeIds?: PersistedIdList;
  familyMemberIds?: PersistedIdList;
};
type PersistedReportWorkspaceState = {
  version: 1;
  activeConfigKey: string;
  workingConfig: PersistedWorkingConfig | null;
  reportRowFilter: string;
  chartCategoryFilter: string;
  reportSectionFilter: SectionFilter;
  chartSectionFilter: ChartSectionFilter;
  showChartLegend: boolean;
};

type AxisChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  stroke: ApexStroke;
  dataLabels: ApexDataLabels;
  legend: ApexLegend;
  tooltip: ApexTooltip;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  plotOptions: ApexPlotOptions;
  colors: string[];
};

type PieChartOptions = {
  series: number[];
  chart: ApexChart;
  labels: string[];
  legend: ApexLegend;
  dataLabels: ApexDataLabels;
  tooltip: ApexTooltip;
  colors: string[];
};

type RowHistoryChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  stroke: ApexStroke;
  dataLabels: ApexDataLabels;
  tooltip: ApexTooltip;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  plotOptions: ApexPlotOptions;
  markers: ApexMarkers;
  colors: string[];
};

@Component({
  selector: 'jhi-income-expenses-report',
  templateUrl: './income-expenses-report.component.html',
  styleUrls: ['./income-expenses-report.component.scss'],
  imports: [SharedModule, DialogModule, NgApexchartsModule],
})
export class IncomeExpensesReportComponent implements OnInit {
  private static readonly WORKSPACE_STORAGE_KEY = 'finance-report-income-expenses-workspace';
  protected readonly reportKey = 'income-expenses';
  protected readonly viewOptions: ViewOption[] = [
    { label: 'Report', value: 'report' },
    { label: 'Bar Chart', value: 'bar' },
    { label: 'Line Chart', value: 'line' },
    { label: 'Pie Chart', value: 'pie' },
  ];
  protected readonly dateOptions: DateOption[] = [
    // { label: '1M', value: '1M' },
    { label: '3M', value: '3M' },
    { label: '6M', value: '6M' },
    { label: '12M', value: '12M' },
    { label: '2Y', value: '2Y' },
    { label: '5Y', value: '5Y' },
    // { label: '7Y', value: '7Y' },
    { label: '10Y', value: '10Y' },
    // { label: 'YTD', value: 'YTD' },
    { label: 'ALL', value: 'ALL' },
    { label: 'Custom', value: 'custom' },
  ];
  protected readonly sectionFilterOptions: SectionFilterOption[] = [
    { label: 'All', value: 'all' },
    { label: 'Income', value: 'income' },
    { label: 'Expense', value: 'expense' },
  ];
  protected readonly chartSectionFilterOptions: ChartSectionFilterOption[] = [
    { label: 'Income', value: 'income' },
    { label: 'Expense', value: 'expense' },
  ];
  protected readonly rowOptions: { label: string; value: ReportRowDimension }[] = [
    { label: 'Type', value: 'categoryType' },
    { label: 'Categories', value: 'category' },
    { label: 'Subcategories', value: 'subcategory' },
  ];
  protected readonly columnOptions = [
    { label: 'Half-months', value: 'halfMonth' },
    { label: 'Months', value: 'month' },
    { label: 'Quarters', value: 'quarter' },
    { label: 'Years', value: 'year' },
  ];
  protected readonly configTabs: { label: string; value: ConfigTab }[] = [
    { label: 'Layout', value: 'layout' },
    { label: 'Accounts', value: 'accounts' },
    { label: 'Categories', value: 'categories' },
    { label: 'Payees', value: 'payees' },
    { label: 'Family Members', value: 'family' },
    { label: 'Title', value: 'title' },
    { label: 'Default View', value: 'view' },
  ];

  protected definition: ReportDefinition | null = null;
  protected savedConfigs: ReportConfig[] = [];
  protected workingConfig: ReportConfig | null = null;
  protected result: ReportResult | null = null;
  protected accounts: FinancialAccount[] = [];
  protected categories: ManagedCategory[] = [];
  protected payees: ManagedPayee[] = [];
  protected familyTree: ReportTreeOption[] = [];
  protected categoryOptions: ReportFilterOption[] = [];
  protected familyOptions: ReportFilterOption[] = [];
  protected axisChartOptions: Partial<AxisChartOptions> = {};
  protected pieChartOptions: Partial<PieChartOptions> = {};
  protected isLoading = false;
  protected isRunning = false;
  protected isSaving = false;
  protected errorMessage: string | null = null;
  protected menuOpen = false;
  protected configDialogVisible = false;
  protected saveDialogVisible = false;
  protected saveDialogMode: 'save' | 'saveAs' = 'save';
  protected saveName = '';
  protected activeConfigKey = 'builtin';
  protected activeConfigTab: ConfigTab = 'layout';
  protected drilldownVisible = false;
  protected drilldownLoading = false;
  protected drilldownResult: ReportDrilldownResult | null = null;
  protected rowHistoryVisible = false;
  protected rowHistoryTrendlineVisible = false;
  protected selectedRowHistory: ReportResultRow | null = null;
  protected rowHistoryChartOptions: Partial<RowHistoryChartOptions> = {};
  protected chartOptimizationSummary: string | null = null;
  protected reportRowFilter = '';
  protected chartCategoryFilter = '';
  protected reportSectionFilter: SectionFilter = 'all';
  protected chartSectionFilter: ChartSectionFilter = 'income';
  protected showChartLegend = false;
  protected readonly optionFilters: Record<FilterListKey, string> = {
    accountIds: '',
    categoryIds: '',
    payeeIds: '',
    familyMemberIds: '',
  };

  private readonly reportsService = inject(ReportsService);
  private readonly accountListService = inject(AccountList);
  private readonly manageDataService = inject(FinanceManageDataService);
  private readonly transactionsService = inject(Transactions);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly cookieService = inject(CookieService);

  constructor(
    @Inject(LOCALE_ID) private readonly locale: string,
    @Inject(DEFAULT_CURRENCY_CODE) private readonly defaultCurrencyCode: string,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  get selectedView(): ViewOption | null {
    if (!this.workingConfig) {
      return null;
    }
    return this.viewOptions.find(option => option.value === this.workingConfig!.defaultView) ?? this.viewOptions[0];
  }

  get selectedDatePreset(): DateOption | null {
    if (!this.workingConfig) {
      return null;
    }
    return this.dateOptions.find(option => option.value === this.workingConfig!.datePreset) ?? this.dateOptions[2];
  }

  get hasReportData(): boolean {
    return !!this.result && this.result.rows.length > 0;
  }

  get filteredResultRows(): ReportResultRow[] {
    if (!this.result) {
      return [];
    }

    const query = this.reportRowFilter.trim().toLowerCase();
    return this.result.rows
      .filter(row => this.matchesSectionFilter(row))
      .filter(row =>
        !query
          ? true
          : [row.label, row.groupLabel, row.parentLabel, row.rowType]
              .filter(Boolean)
              .some(value => String(value).toLowerCase().includes(query)),
      );
  }

  get filteredChartRows(): ReportResultRow[] {
    if (!this.result) {
      return [];
    }

    const query = this.chartCategoryFilter.trim().toLowerCase();
    return this.result.rows
      .filter(row => !row.subtotal && !row.grandTotal && row.rowType !== 'sectionTotal' && row.rowType !== 'net')
      .filter(row => this.matchesSectionFilter(row, this.activeChartSectionFilter))
      .filter(row =>
        !query
          ? true
          : [row.label, row.groupLabel, row.parentLabel].filter(Boolean).some(value => String(value).toLowerCase().includes(query)),
      );
  }

  get hasFilteredAxisSeries(): boolean {
    return (this.axisChartOptions.series?.length ?? 0) > 0;
  }

  get hasFilteredPieSegments(): boolean {
    return (this.pieChartOptions.series?.length ?? 0) > 0;
  }

  get activeChartSectionFilter(): SectionFilter {
    return this.workingConfig?.defaultView === 'bar' || this.workingConfig?.defaultView === 'pie'
      ? this.chartSectionFilter
      : this.reportSectionFilter;
  }

  setReportSectionFilter(filter: SectionFilter): void {
    this.reportSectionFilter = filter;
    this.syncCharts();
    this.saveWorkspaceState();
  }

  setChartSectionFilter(filter: ChartSectionFilter): void {
    this.chartSectionFilter = filter;
    this.syncCharts();
    this.saveWorkspaceState();
  }

  clearChartCategoryFilter(): void {
    this.chartCategoryFilter = '';
    this.syncCharts();
    this.saveWorkspaceState();
  }

  onChartCategoryFilterChanged(): void {
    this.syncCharts();
    this.saveWorkspaceState();
  }

  onChartLegendChanged(): void {
    this.syncCharts();
    this.saveWorkspaceState();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = null;

    forkJoin({
      definitions: this.reportsService.getDefinitions(),
      configs: this.reportsService.getConfigs(this.reportKey),
      accounts: this.accountListService.getSimple(),
      categories: this.manageDataService.getCategories(),
      payees: this.manageDataService.getPayees(false),
      familyTree: this.transactionsService.getWhoTreeOptions(),
    }).subscribe({
      next: response => {
        this.definition = response.definitions.find(definition => definition.key === this.reportKey) ?? null;
        this.savedConfigs = response.configs;
        this.accounts = response.accounts;
        this.categories = response.categories;
        this.payees = response.payees;
        this.familyTree = this.normalizeTreeOptions(response.familyTree);
        this.categoryOptions = this.buildCategoryOptions(response.categories);
        this.familyOptions = this.flattenFamilyOptions(this.familyTree);

        if (!this.definition) {
          this.errorMessage = 'The Income & Expenses report definition could not be loaded.';
          this.isLoading = false;
          this.changeDetectorRef.markForCheck();
          return;
        }

        this.workingConfig = this.enrichDefaultConfig(this.definition.defaultConfig);
        this.activeConfigKey = 'builtin';
        this.restoreWorkspaceState();
        this.syncCharts();
        this.isLoading = false;
        this.runReport();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Loading reports failed.');
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  onDatePresetChanged(option: DateOption | null): void {
    if (!option || !this.workingConfig) {
      return;
    }
    this.workingConfig.datePreset = option.value;
    if (option.value !== 'custom') {
      this.workingConfig.startDate = null;
      this.workingConfig.endDate = null;
    } else {
      const [startDate, endDate] = this.resolvePresetDates('6M');
      this.workingConfig.startDate = startDate;
      this.workingConfig.endDate = endDate;
    }
    this.saveWorkspaceState();
    this.runReport();
  }

  onViewChanged(option: ViewOption | null): void {
    if (!option || !this.workingConfig) {
      return;
    }
    this.workingConfig.defaultView = option.value;
    this.syncCharts();
    this.saveWorkspaceState();
  }

  onRowDimensionChanged(value: ReportRowDimension | null): void {
    if (!value || !this.workingConfig || this.workingConfig.rowDimension === value) {
      return;
    }
    this.workingConfig.rowDimension = value;
    this.onConfigFieldChanged();
  }

  onCustomDatesChanged(): void {
    this.saveWorkspaceState();
    this.runReport();
  }

  onConfigSelected(configKey: string): void {
    this.menuOpen = false;
    this.activeConfigKey = configKey;

    if (configKey === 'builtin') {
      this.workingConfig = this.enrichDefaultConfig(this.definition!.defaultConfig);
    } else {
      const selected = this.savedConfigs.find(config => config.id === configKey);
      if (selected) {
        this.workingConfig = this.cloneConfig(selected);
      }
    }

    this.activeConfigTab = 'layout';
    this.saveWorkspaceState();
    this.runReport();
  }

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  openConfigDialog(): void {
    this.menuOpen = false;
    this.configDialogVisible = true;
  }

  closeConfigDialog(): void {
    this.configDialogVisible = false;
  }

  onConfigFieldChanged(): void {
    this.saveWorkspaceState();
    this.runReport();
  }

  onCheckboxListToggle(listKey: FilterListKey, id: string, checked: boolean): void {
    if (!this.workingConfig) {
      return;
    }
    const current = new Set(this.workingConfig[listKey]);
    if (checked) {
      current.add(id);
    } else {
      current.delete(id);
    }
    this.workingConfig[listKey] = Array.from(current);
    this.saveWorkspaceState();
    this.runReport();
  }

  selectAllCategories(): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig.categoryIds = this.categoryOptions.map(option => option.id);
    this.saveWorkspaceState();
    this.runReport();
  }

  selectIncomeCategories(): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig.categoryIds = this.categoryOptions.filter(option => option.rootType === 'Income').map(option => option.id);
    this.saveWorkspaceState();
    this.runReport();
  }

  selectExpenseCategories(): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig.categoryIds = this.categoryOptions.filter(option => option.rootType === 'Expense').map(option => option.id);
    this.saveWorkspaceState();
    this.runReport();
  }

  clearAllCategories(): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig.categoryIds = [];
    this.saveWorkspaceState();
    this.runReport();
  }

  selectAllList(listKey: Exclude<FilterListKey, 'categoryIds'>): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig[listKey] = this.getOptionsForList(listKey).map(option => option.id);
    this.saveWorkspaceState();
    this.runReport();
  }

  clearList(listKey: Exclude<FilterListKey, 'categoryIds'>): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig[listKey] = [];
    this.saveWorkspaceState();
    this.runReport();
  }

  saveCurrentConfig(): void {
    if (!this.workingConfig) {
      return;
    }

    if (this.workingConfig.builtin || !this.workingConfig.id) {
      this.openSaveDialog('saveAs');
      return;
    }

    this.saveDialogMode = 'save';
    this.saveName = this.workingConfig.name;
    this.persistConfig(this.saveName);
  }

  openSaveDialog(mode: 'save' | 'saveAs'): void {
    if (!this.workingConfig) {
      return;
    }
    this.menuOpen = false;
    this.saveDialogMode = mode;
    this.saveName = mode === 'saveAs' || this.workingConfig.builtin ? '' : this.workingConfig.name;
    this.saveDialogVisible = true;
  }

  confirmSaveDialog(): void {
    this.persistConfig(this.saveName);
  }

  private persistConfig(name: string): void {
    if (!this.workingConfig) {
      return;
    }

    const trimmedName = name.trim();
    if (!trimmedName) {
      this.errorMessage = 'A unique configuration name is required.';
      this.changeDetectorRef.markForCheck();
      return;
    }

    const payload = this.buildExecutionConfig(this.workingConfig);
    payload.name = trimmedName;
    payload.builtin = false;
    payload.editable = true;

    this.isSaving = true;
    this.errorMessage = null;

    const request$ =
      this.saveDialogMode === 'save' && payload.id
        ? this.reportsService.updateConfig(payload)
        : this.reportsService.createConfig({ ...payload, id: null });

    request$.subscribe({
      next: config => {
        const existingIndex = this.savedConfigs.findIndex(item => item.id === config.id);
        if (existingIndex >= 0) {
          this.savedConfigs.splice(existingIndex, 1, config);
        } else {
          this.savedConfigs = [...this.savedConfigs, config].sort((left, right) => left.name.localeCompare(right.name));
        }
        this.workingConfig = this.cloneConfig(config);
        this.activeConfigKey = config.id ?? 'builtin';
        this.saveDialogVisible = false;
        this.isSaving = false;
        this.saveWorkspaceState();
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Saving the report configuration failed.');
        this.isSaving = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  runReport(): void {
    if (!this.workingConfig) {
      return;
    }

    this.saveWorkspaceState();

    if (this.shouldShowEmptyState()) {
      this.result = this.buildLocalEmptyResult();
      this.syncCharts();
      this.changeDetectorRef.markForCheck();
      return;
    }

    const payload = this.cloneConfig(this.workingConfig);
    if (payload.datePreset === 'custom' && (!payload.startDate || !payload.endDate)) {
      return;
    }

    this.isRunning = true;
    this.errorMessage = null;
    this.reportsService.runIncomeExpenses(payload).subscribe({
      next: result => {
        this.result = result;
        this.isRunning = false;
        this.syncCharts();
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Running the report failed.');
        this.isRunning = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  formatCellValue(value: number, percentValue: number): string {
    if (!this.result) {
      return '';
    }
    return this.result.showPercentOfTotal ? `${(percentValue * 100).toFixed(1)}%` : this.formatCurrency(value);
  }

  openCellDrilldown(row: ReportResultRow, column?: ReportResultColumn): void {
    if (!this.workingConfig) {
      return;
    }

    this.drilldownVisible = true;
    this.drilldownLoading = true;
    this.drilldownResult = null;

    this.reportsService
      .getIncomeExpenseDrilldown({
        config: this.buildExecutionConfig(this.workingConfig),
        rowKey: row.key,
        rowLabel: row.label,
        columnKey: column?.key ?? null,
        columnLabel: column?.label ?? 'Total',
      })
      .subscribe({
        next: result => {
          this.drilldownResult = result;
          this.drilldownLoading = false;
          this.changeDetectorRef.markForCheck();
        },
        error: error => {
          this.errorMessage = this.getErrorMessage(error, 'Loading the report transactions failed.');
          this.drilldownLoading = false;
          this.drilldownVisible = false;
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  closeDrilldown(): void {
    this.drilldownVisible = false;
    this.drilldownLoading = false;
    this.drilldownResult = null;
  }

  openRowHistory(row: ReportResultRow): void {
    if (!this.result || this.result.columns.length === 0) {
      return;
    }
    this.selectedRowHistory = row;
    this.rowHistoryTrendlineVisible = false;
    this.rowHistoryVisible = true;
    this.syncRowHistoryChart();
  }

  closeRowHistory(): void {
    this.rowHistoryVisible = false;
    this.rowHistoryTrendlineVisible = false;
    this.selectedRowHistory = null;
    this.rowHistoryChartOptions = {};
  }

  toggleRowHistoryTrendline(): void {
    this.rowHistoryTrendlineVisible = !this.rowHistoryTrendlineVisible;
    this.syncRowHistoryChart();
  }

  get rowHistoryAverage(): number {
    if (!this.selectedRowHistory || this.selectedRowHistory.values.length === 0) {
      return 0;
    }
    return this.selectedRowHistory.total / this.selectedRowHistory.values.length;
  }

  getAccountName(accountId: string | null | undefined): string {
    if (!accountId) {
      return 'Unassigned';
    }
    return this.accounts.find(account => account.id === accountId)?.name ?? accountId;
  }

  isSelected(listKey: 'accountIds' | 'categoryIds' | 'payeeIds' | 'familyMemberIds', id: string): boolean {
    return !!this.workingConfig?.[listKey].includes(id);
  }

  clearOptionFilter(listKey: FilterListKey): void {
    this.optionFilters[listKey] = '';
  }

  get filteredAccountOptions(): ReportFilterOption[] {
    return this.filterOptions(
      this.accounts.map(account => ({ id: account.id, label: account.name })),
      this.optionFilters.accountIds,
    );
  }

  get filteredCategoryOptions(): ReportFilterOption[] {
    return this.filterOptions(this.categoryOptions, this.optionFilters.categoryIds);
  }

  get filteredPayeeOptions(): ReportFilterOption[] {
    return this.filterOptions(
      this.payees.map(payee => ({ id: payee.id, label: payee.name })),
      this.optionFilters.payeeIds,
    );
  }

  get filteredFamilyOptions(): ReportFilterOption[] {
    return this.filterOptions(this.familyOptions, this.optionFilters.familyMemberIds);
  }

  trackById(_index: number, item: { id?: string | null; key?: string }): string {
    return item.id ?? item.key ?? String(_index);
  }

  private buildCategoryOptions(categories: ManagedCategory[]): ReportFilterOption[] {
    const categoryOnly = categories.filter(category => category.classificationId === 0);
    const byId = new Map(categoryOnly.map(category => [category.id, category]));

    return categoryOnly.map(category => ({
      id: category.id,
      label: category.displayName,
      parentId: category.parentId,
      rootType: this.findRootType(category, byId),
    }));
  }

  private findRootType(category: ManagedCategory, byId: Map<string, ManagedCategory>): 'Income' | 'Expense' | null {
    let current: ManagedCategory | undefined = category;
    let rootName = category.name;

    while (current?.parentId) {
      current = byId.get(current.parentId);
      if (!current) {
        break;
      }
      rootName = current.name;
    }

    if (rootName.toLowerCase() === 'income') {
      return 'Income';
    }
    if (rootName.toLowerCase() === 'expense') {
      return 'Expense';
    }
    return null;
  }

  private flattenFamilyOptions(tree: ReportTreeOption[], prefix = ''): ReportFilterOption[] {
    return tree.flatMap(node => {
      const label = prefix ? `${prefix} / ${node.label}` : node.label;
      const current = node.selectable ? [{ id: node.key, label }] : [];
      const children = this.flattenFamilyOptions(node.children ?? [], label);
      return [...current, ...children];
    });
  }

  private normalizeTreeOptions(tree: TransactionTreeOption[]): ReportTreeOption[] {
    return tree.map(node => ({
      key: node.key,
      label: node.label,
      selectable: node.selectable ?? true,
      leaf: node.leaf ?? false,
      children: this.normalizeTreeOptions(node.children ?? []),
    }));
  }

  private enrichDefaultConfig(config: ReportConfig): ReportConfig {
    const clone = this.cloneConfig(config);
    clone.accountIds = this.accounts.map(account => account.id);
    clone.categoryIds = this.categoryOptions.map(option => option.id);
    clone.payeeIds = this.payees.map(payee => payee.id);
    clone.familyMemberIds = this.familyOptions.map(option => option.id);
    if (clone.datePreset === 'custom' && (!clone.startDate || !clone.endDate)) {
      const [startDate, endDate] = this.resolvePresetDates('6M');
      clone.startDate = startDate;
      clone.endDate = endDate;
    }
    return clone;
  }

  private cloneConfig(config: ReportConfig): ReportConfig {
    return {
      ...config,
      id: config.id ?? null,
      accountIds: [...config.accountIds],
      categoryIds: [...config.categoryIds],
      payeeIds: [...config.payeeIds],
      familyMemberIds: [...config.familyMemberIds],
      startDate: config.startDate ?? null,
      endDate: config.endDate ?? null,
    };
  }

  private buildExecutionConfig(config: ReportConfig): ReportConfig {
    const clone = this.cloneConfig(config);
    clone.accountIds = this.compactExecutionIds(
      clone.accountIds,
      this.accounts.map(account => account.id),
    );
    clone.categoryIds = this.compactExecutionIds(
      clone.categoryIds,
      this.categoryOptions.map(option => option.id),
    );
    clone.payeeIds = this.compactExecutionIds(
      clone.payeeIds,
      this.payees.map(payee => payee.id),
    );
    clone.familyMemberIds = this.compactExecutionIds(
      clone.familyMemberIds,
      this.familyOptions.map(option => option.id),
    );
    return clone;
  }

  private resolvePresetDates(preset: Exclude<ReportDatePreset, 'custom'>): [string, string] {
    const endDate = new Date();
    const startDate = new Date(endDate);

    switch (preset) {
      case '1M':
        startDate.setMonth(startDate.getMonth() - 1);
        startDate.setDate(startDate.getDate() + 1);
        break;
      case '3M':
        startDate.setMonth(startDate.getMonth() - 3);
        startDate.setDate(startDate.getDate() + 1);
        break;
      case '6M':
        startDate.setMonth(startDate.getMonth() - 6);
        startDate.setDate(startDate.getDate() + 1);
        break;
      case '12M':
        startDate.setFullYear(startDate.getFullYear() - 1);
        startDate.setDate(startDate.getDate() + 1);
        break;
      case '2Y':
        startDate.setFullYear(startDate.getFullYear() - 2);
        startDate.setDate(startDate.getDate() + 1);
        break;
      case '5Y':
        startDate.setFullYear(startDate.getFullYear() - 5);
        startDate.setDate(startDate.getDate() + 1);
        break;
      case '7Y':
        startDate.setFullYear(startDate.getFullYear() - 7);
        startDate.setDate(startDate.getDate() + 1);
        break;
      case '10Y':
        startDate.setFullYear(startDate.getFullYear() - 10);
        startDate.setDate(startDate.getDate() + 1);
        break;
      case 'YTD':
        startDate.setMonth(0, 1);
        break;
      case 'ALL':
        startDate.setFullYear(2000, 0, 1);
        break;
    }

    return [this.toDateInputValue(startDate), this.toDateInputValue(endDate)];
  }

  private shouldShowEmptyState(): boolean {
    if (!this.workingConfig) {
      return false;
    }

    return (
      (this.accounts.length > 0 && this.workingConfig.accountIds.length === 0) ||
      (this.categoryOptions.length > 0 && this.workingConfig.categoryIds.length === 0) ||
      (this.payees.length > 0 && this.workingConfig.payeeIds.length === 0) ||
      (this.familyOptions.length > 0 && this.workingConfig.familyMemberIds.length === 0)
    );
  }

  private buildLocalEmptyResult(): ReportResult {
    return {
      title: this.workingConfig?.title ?? 'Income & Expenses',
      currencyCode: this.result?.currencyCode ?? 'AUD',
      rowDimension: this.workingConfig?.rowDimension ?? 'subcategory',
      columnDimension: this.workingConfig?.columnDimension ?? 'month',
      defaultView: this.workingConfig?.defaultView ?? 'report',
      showPercentOfTotal: this.workingConfig?.showPercentOfTotal ?? false,
      startDate: this.workingConfig?.startDate ?? this.resolvePresetDates('6M')[0],
      endDate: this.workingConfig?.endDate ?? this.resolvePresetDates('6M')[1],
      grandTotal: 0,
      columns: [],
      rows: [],
      series: [],
      pie: [],
    };
  }

  private syncCharts(): void {
    const columnLabels = this.result?.columns.map(column => column.label) ?? [];
    const palette = ['#1f4c7a', '#4f8fba', '#7bb4d8', '#c65d3b', '#e8a25c', '#6b8f71', '#9ab87a'];
    const filteredRows = this.filteredChartRows;
    const filteredSeries = this.buildAxisChartSeries(filteredRows, columnLabels.length);
    const allowedSeriesNames = new Set(filteredSeries.map(series => series.name));
    const filteredPie = (this.result?.pie ?? []).filter(segment => allowedSeriesNames.has(segment.label));

    this.axisChartOptions = {
      series: filteredSeries.map(series => ({ name: series.name, data: series.data })),
      chart: {
        type: this.workingConfig?.defaultView === 'line' ? 'line' : 'bar',
        toolbar: { show: false },
        height: 380,
        animations: {
          enabled: this.workingConfig?.defaultView !== 'bar' || filteredSeries.length * columnLabels.length <= 220,
        },
      },
      stroke: {
        curve: 'smooth',
        width: this.workingConfig?.defaultView === 'line' ? 3 : 1,
      },
      dataLabels: { enabled: false },
      legend: { show: this.showChartLegend, position: 'right' },
      tooltip: {
        y: {
          formatter: value => this.formatCurrency(Number(value ?? 0)),
          title: {
            formatter: seriesName => this.resolveChartTooltipLabel(seriesName),
          },
        },
      },
      xaxis: { categories: columnLabels },
      yaxis: {
        labels: {
          formatter: value => (this.result?.showPercentOfTotal ? `${Number(value) * 100}%` : this.formatCurrency(Number(value ?? 0))),
        },
      },
      plotOptions: {
        bar: {
          columnWidth: '58%',
          borderRadius: 4,
        },
      },
      colors: palette,
    };

    this.pieChartOptions = {
      series: filteredPie.map(segment => segment.value),
      chart: { type: 'donut', height: 380 },
      labels: filteredPie.map(segment => segment.label),
      legend: { position: 'right', show: this.showChartLegend },
      dataLabels: { enabled: true },
      tooltip: {
        y: {
          formatter: value => this.formatCurrency(Number(value ?? 0)),
          title: {
            formatter: seriesName => this.resolveChartTooltipLabel(seriesName),
          },
        },
      },
      colors: palette,
    };

    this.syncRowHistoryChart();
  }

  private buildAxisChartSeries(rows: ReportResultRow[], columnCount: number): { name: string; data: number[] }[] {
    const baseRows: ChartAggregateRow[] = rows.map(row => ({
      label: row.label,
      section: this.resolveRowSection(row),
      total: row.total,
      values: [...row.values],
    }));

    if (this.workingConfig?.defaultView !== 'bar') {
      this.chartOptimizationSummary = null;
      return baseRows.map(row => ({ name: row.label, data: row.values }));
    }

    if (!this.shouldOptimizeBarChart(baseRows.length, columnCount)) {
      this.chartOptimizationSummary = null;
      return baseRows.map(row => ({ name: row.label, data: row.values }));
    }

    let aggregatedRows = this.rollUpBarChartRows(baseRows);
    let summary = this.buildBarRollupSummary();

    if (aggregatedRows.length > 14) {
      aggregatedRows = this.collapseMinorBarChartRows(aggregatedRows, 12);
      summary += ' Minor groups are combined into Other.';
    }

    this.chartOptimizationSummary = summary;
    return aggregatedRows.map(row => ({ name: row.label, data: row.values }));
  }

  private shouldOptimizeBarChart(rowCount: number, columnCount: number): boolean {
    return columnCount > 24 || rowCount > 18 || rowCount * columnCount > 260;
  }

  private rollUpBarChartRows(rows: ChartAggregateRow[]): ChartAggregateRow[] {
    if (!this.workingConfig || rows.length === 0 || this.workingConfig.rowDimension === 'categoryType') {
      return rows;
    }

    const sourceRows = new Map(this.filteredChartRows.map(row => [row.label, row] as const));
    const rollups = new Map<string, ChartAggregateRow>();

    for (const row of rows) {
      const sourceRow = sourceRows.get(row.label);
      const rollupLabel =
        this.workingConfig.rowDimension === 'subcategory'
          ? (sourceRow?.groupLabel ?? sourceRow?.parentLabel ?? row.label)
          : this.formatSectionLabel(row.section);
      const key = `${row.section}::${rollupLabel}`;
      const existing = rollups.get(key);

      if (existing) {
        existing.total += row.total;
        existing.values = existing.values.map((value, index) => value + (row.values[index] ?? 0));
      } else {
        rollups.set(key, {
          label: rollupLabel,
          section: row.section,
          total: row.total,
          values: [...row.values],
        });
      }
    }

    return Array.from(rollups.values()).sort((left, right) => {
      const sectionCompare = this.compareSectionOrder(left.section, right.section);
      if (sectionCompare !== 0) {
        return sectionCompare;
      }
      return Math.abs(right.total) - Math.abs(left.total);
    });
  }

  private collapseMinorBarChartRows(rows: ChartAggregateRow[], maxRows: number): ChartAggregateRow[] {
    const rowsBySection = new Map<string, ChartAggregateRow[]>();
    for (const row of rows) {
      const sectionRows = rowsBySection.get(row.section) ?? [];
      sectionRows.push(row);
      rowsBySection.set(row.section, sectionRows);
    }

    const sections = Array.from(rowsBySection.entries()).sort(([left], [right]) => this.compareSectionOrder(left, right));
    const preservedPerSection = Math.max(1, Math.floor(maxRows / Math.max(1, sections.length)));
    const collapsedRows: ChartAggregateRow[] = [];

    for (const [section, sectionRows] of sections) {
      const ordered = [...sectionRows].sort((left, right) => Math.abs(right.total) - Math.abs(left.total));
      const keep = ordered.slice(0, preservedPerSection);
      const remainder = ordered.slice(preservedPerSection);
      collapsedRows.push(...keep);

      if (remainder.length > 0) {
        const otherValues = new Array(ordered[0]?.values.length ?? 0).fill(0);
        let otherTotal = 0;
        for (const row of remainder) {
          otherTotal += row.total;
          row.values.forEach((value, index) => {
            otherValues[index] += value ?? 0;
          });
        }

        collapsedRows.push({
          label: `Other ${this.formatSectionLabel(section)}`,
          section,
          total: otherTotal,
          values: otherValues,
        });
      }
    }

    return collapsedRows;
  }

  private buildBarRollupSummary(): string {
    if (!this.workingConfig) {
      return 'Bar chart simplified for performance.';
    }

    if (this.workingConfig.rowDimension === 'subcategory') {
      return 'Bar chart simplified to categories for performance.';
    }

    if (this.workingConfig.rowDimension === 'category') {
      return 'Bar chart simplified to category types for performance.';
    }

    return 'Bar chart simplified for performance.';
  }

  private resolveRowSection(row: Pick<ReportResultRow, 'parentLabel' | 'groupLabel' | 'label'>): string {
    const section = String(row.parentLabel ?? row.groupLabel ?? row.label ?? '')
      .trim()
      .toLowerCase();
    return section === 'income' ? 'income' : 'expense';
  }

  private compareSectionOrder(left: string, right: string): number {
    const rank = (section: string): number => (section === 'income' ? 0 : 1);
    return rank(left) - rank(right);
  }

  private formatSectionLabel(section: string): string {
    return section === 'income' ? 'Income' : 'Expense';
  }

  private syncRowHistoryChart(): void {
    if (!this.result || !this.selectedRowHistory) {
      this.rowHistoryChartOptions = {};
      return;
    }

    const labels = this.result.columns.map(column => column.label);
    const values = this.selectedRowHistory.values.map(value => Number(value ?? 0));
    const series: ApexAxisChartSeries = [
      {
        name: this.selectedRowHistory.label,
        data: values,
        type: 'bar',
      } as ApexAxisChartSeries[number],
    ];

    const trendlineValues = this.rowHistoryTrendlineVisible ? this.buildTrendlineValues(values) : null;
    if (trendlineValues) {
      series.push({
        name: this.buildTrendlineLabel(values, trendlineValues),
        data: trendlineValues,
        type: 'line',
      } as ApexAxisChartSeries[number]);
    }

    this.rowHistoryChartOptions = {
      series,
      chart: {
        type: 'line',
        height: 320,
        toolbar: { show: false },
        zoom: { enabled: false },
        animations: { enabled: false },
      },
      stroke: {
        curve: 'smooth',
        width: trendlineValues ? [0, 3] : [0],
      },
      markers: {
        size: trendlineValues ? [0, 3] : [0],
        hover: { sizeOffset: 2 },
      },
      dataLabels: { enabled: false },
      tooltip: {
        y: {
          formatter: value => this.formatCurrency(Number(value ?? 0)),
        },
      },
      xaxis: { categories: labels },
      yaxis: {
        labels: {
          formatter: value => this.formatCurrency(Number(value ?? 0)),
        },
      },
      plotOptions: {
        bar: {
          columnWidth: '58%',
          borderRadius: 4,
        },
      },
      colors: ['#1f4c7a', '#c65d3b'],
    };
  }

  private restoreWorkspaceState(): void {
    const rawValue = this.readWorkspaceState();
    if (!rawValue || !this.workingConfig) {
      return;
    }

    try {
      const state = JSON.parse(rawValue) as PersistedReportWorkspaceState;
      if (state.version !== 1) {
        return;
      }

      this.reportRowFilter = state.reportRowFilter ?? '';
      this.chartCategoryFilter = state.chartCategoryFilter ?? '';
      this.reportSectionFilter = state.reportSectionFilter ?? 'all';
      this.chartSectionFilter = state.chartSectionFilter ?? 'income';
      this.showChartLegend = !!state.showChartLegend;

      const baseConfig =
        state.activeConfigKey && state.activeConfigKey !== 'builtin'
          ? this.savedConfigs.find(config => config.id === state.activeConfigKey)
          : null;

      if (baseConfig) {
        this.activeConfigKey = state.activeConfigKey;
        this.workingConfig = this.cloneConfig(baseConfig);
      }

      if (state.workingConfig) {
        this.workingConfig = this.mergePersistedConfig(this.workingConfig, state.workingConfig);
      }
    } catch {
      // Ignore invalid cookie data and fall back to the default state.
    }
  }

  private saveWorkspaceState(): void {
    const state: PersistedReportWorkspaceState = {
      version: 1,
      activeConfigKey: this.activeConfigKey,
      workingConfig: this.buildPersistedConfig(),
      reportRowFilter: this.reportRowFilter,
      chartCategoryFilter: this.chartCategoryFilter,
      reportSectionFilter: this.reportSectionFilter,
      chartSectionFilter: this.chartSectionFilter,
      showChartLegend: this.showChartLegend,
    };

    this.writeWorkspaceState(JSON.stringify(state));
  }

  private readWorkspaceState(): string | null {
    const storage = this.getStorage();
    const storedValue = storage?.getItem(IncomeExpensesReportComponent.WORKSPACE_STORAGE_KEY);
    if (storedValue) {
      this.clearLegacyWorkspaceCookie();
      return storedValue;
    }

    const legacyCookie = this.cookieService.get(IncomeExpensesReportComponent.WORKSPACE_STORAGE_KEY);
    if (!legacyCookie) {
      return null;
    }

    this.writeWorkspaceState(legacyCookie);
    this.clearLegacyWorkspaceCookie();
    return legacyCookie;
  }

  private writeWorkspaceState(value: string): void {
    const storage = this.getStorage();
    if (!storage) {
      return;
    }

    try {
      storage.setItem(IncomeExpensesReportComponent.WORKSPACE_STORAGE_KEY, value);
    } catch {
      // Ignore storage quota and browser storage errors.
    }
  }

  private clearLegacyWorkspaceCookie(): void {
    this.cookieService.remove(IncomeExpensesReportComponent.WORKSPACE_STORAGE_KEY);
  }

  private getStorage(): Storage | null {
    try {
      return typeof window !== 'undefined' ? window.localStorage : null;
    } catch {
      return null;
    }
  }

  private buildPersistedConfig(): PersistedWorkingConfig | null {
    if (!this.workingConfig) {
      return null;
    }

    return {
      id: this.workingConfig.id ?? null,
      reportKey: this.workingConfig.reportKey,
      name: this.workingConfig.name,
      title: this.workingConfig.title,
      rowDimension: this.workingConfig.rowDimension,
      columnDimension: this.workingConfig.columnDimension,
      showPercentOfTotal: this.workingConfig.showPercentOfTotal,
      defaultView: this.workingConfig.defaultView,
      datePreset: this.workingConfig.datePreset,
      startDate: this.workingConfig.startDate,
      endDate: this.workingConfig.endDate,
      builtin: this.workingConfig.builtin,
      editable: this.workingConfig.editable,
      accountIds: this.encodePersistedIdList(
        this.workingConfig.accountIds,
        this.accounts.map(account => account.id),
      ),
      categoryIds: this.encodePersistedIdList(
        this.workingConfig.categoryIds,
        this.categoryOptions.map(option => option.id),
      ),
      payeeIds: this.encodePersistedIdList(
        this.workingConfig.payeeIds,
        this.payees.map(payee => payee.id),
      ),
      familyMemberIds: this.encodePersistedIdList(
        this.workingConfig.familyMemberIds,
        this.familyOptions.map(option => option.id),
      ),
    };
  }

  private mergePersistedConfig(current: ReportConfig, persisted: PersistedWorkingConfig): ReportConfig {
    return {
      ...current,
      ...persisted,
      id: persisted.id ?? current.id ?? null,
      accountIds: persisted.accountIds
        ? this.decodePersistedIdList(
            persisted.accountIds,
            this.accounts.map(account => account.id),
          )
        : [...current.accountIds],
      categoryIds: persisted.categoryIds
        ? this.decodePersistedIdList(
            persisted.categoryIds,
            this.categoryOptions.map(option => option.id),
          )
        : [...current.categoryIds],
      payeeIds: persisted.payeeIds
        ? this.decodePersistedIdList(
            persisted.payeeIds,
            this.payees.map(payee => payee.id),
          )
        : [...current.payeeIds],
      familyMemberIds: persisted.familyMemberIds
        ? this.decodePersistedIdList(
            persisted.familyMemberIds,
            this.familyOptions.map(option => option.id),
          )
        : [...current.familyMemberIds],
      startDate: persisted.startDate ?? current.startDate ?? null,
      endDate: persisted.endDate ?? current.endDate ?? null,
    };
  }

  private encodePersistedIdList(selectedIds: string[], allIds: string[]): PersistedIdList {
    const selectedSet = new Set(selectedIds);
    const normalizedSelected = allIds.filter(id => selectedSet.has(id));
    const excluded = allIds.filter(id => !selectedSet.has(id));

    if (excluded.length < normalizedSelected.length) {
      return {
        mode: 'allExcept',
        ids: excluded,
      };
    }

    return {
      mode: 'only',
      ids: normalizedSelected,
    };
  }

  private decodePersistedIdList(persisted: PersistedIdList, allIds: string[]): string[] {
    if (persisted.mode === 'allExcept') {
      const excluded = new Set(persisted.ids);
      return allIds.filter(id => !excluded.has(id));
    }

    const allowed = new Set(allIds);
    return persisted.ids.filter(id => allowed.has(id));
  }

  private compactExecutionIds(selectedIds: string[], allIds: string[]): string[] {
    if (selectedIds.length === 0 || allIds.length === 0) {
      return [...selectedIds];
    }

    const available = new Set(allIds);
    const normalizedSelected = selectedIds.filter(id => available.has(id));
    if (normalizedSelected.length !== allIds.length) {
      return normalizedSelected;
    }

    const selectedSet = new Set(normalizedSelected);
    return allIds.every(id => selectedSet.has(id)) ? [] : normalizedSelected;
  }

  private getOptionsForList(listKey: Exclude<FilterListKey, 'categoryIds'>): ReportFilterOption[] {
    if (listKey === 'accountIds') {
      return this.accounts.map(account => ({ id: account.id, label: account.name }));
    }
    if (listKey === 'payeeIds') {
      return this.payees.map(payee => ({ id: payee.id, label: payee.name }));
    }
    return this.familyOptions;
  }

  private toDateInputValue(date: Date): string {
    return date.toISOString().slice(0, 10);
  }

  private filterOptions(options: ReportFilterOption[], query: string): ReportFilterOption[] {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) {
      return options;
    }

    return options.filter(option => option.label.toLowerCase().includes(normalizedQuery));
  }

  formatCurrency(value: number): string {
    return formatCurrencyAmount(value ?? 0, this.locale, this.result?.currencyCode ?? this.defaultCurrencyCode);
  }

  formatCurrencyWithCode(value: number, currencyCode: string): string {
    return formatCurrencyAmount(value ?? 0, this.locale, currencyCode || this.defaultCurrencyCode);
  }

  isForeignCurrencyDrilldownTransaction(transaction: ReportDrilldownTransaction, baseCurrencyCode: string | null | undefined): boolean {
    const originalCurrencyCode = transaction.originalCurrencyCode?.trim();
    const normalizedBaseCurrency = (baseCurrencyCode ?? this.defaultCurrencyCode).trim();
    return !!originalCurrencyCode && originalCurrencyCode !== normalizedBaseCurrency;
  }

  getDrilldownCurrencyHint(transaction: ReportDrilldownTransaction, baseCurrencyCode: string | null | undefined): string | null {
    if (!this.isForeignCurrencyDrilldownTransaction(transaction, baseCurrencyCode) || transaction.originalAmount == null) {
      return null;
    }

    const currencyCode = transaction.originalCurrencyCode?.trim() || this.defaultCurrencyCode;
    return `Original amount (${currencyCode}): ${this.formatCurrencyWithCode(transaction.originalAmount, currencyCode)}`;
  }

  private matchesSectionFilter(row: ReportResultRow, filter: SectionFilter = this.reportSectionFilter): boolean {
    if (filter === 'all') {
      return true;
    }

    const section = String(row.parentLabel ?? row.groupLabel ?? row.label ?? '')
      .trim()
      .toLowerCase();

    if (filter === 'income') {
      return section === 'income' || row.key === 'section-total:income';
    }

    return section === 'expense' || section === 'expenses' || row.key === 'section-total:expense' || row.key === 'section-total:expenses';
  }

  private resolveChartTooltipLabel(label: string): string {
    if (this.workingConfig?.rowDimension !== 'subcategory') {
      return label;
    }

    const matchingRow = this.filteredChartRows.find(row => row.label === label);
    if (!matchingRow?.groupLabel) {
      return label;
    }

    return `${matchingRow.groupLabel} / ${matchingRow.label}`;
  }

  private buildTrendlineValues(values: number[]): number[] | null {
    if (values.length < 2) {
      return null;
    }

    const count = values.length;
    const sumX = values.reduce((sum, _value, index) => sum + index, 0);
    const sumY = values.reduce((sum, value) => sum + value, 0);
    const sumXY = values.reduce((sum, value, index) => sum + index * value, 0);
    const sumXX = values.reduce((sum, _value, index) => sum + index * index, 0);
    const denominator = count * sumXX - sumX * sumX;

    if (denominator === 0) {
      return null;
    }

    const slope = (count * sumXY - sumX * sumY) / denominator;
    const intercept = (sumY - slope * sumX) / count;
    return values.map((_value, index) => slope * index + intercept);
  }

  private buildTrendlineLabel(values: number[], trendlineValues: number[]): string {
    const meanY = values.reduce((sum, value) => sum + value, 0) / values.length;
    const ssTot = values.reduce((sum, value) => sum + (value - meanY) ** 2, 0);
    const ssRes = values.reduce((sum, value, index) => sum + (value - trendlineValues[index]) ** 2, 0);
    const rSquared = ssTot === 0 ? 1 : Math.max(0, 1 - ssRes / ssTot);
    return `Trendline (R² ${rSquared.toFixed(2)})`;
  }

  private getErrorMessage(error: any, fallback: string): string {
    return error?.error?.detail || error?.error?.message || fallback;
  }
}
