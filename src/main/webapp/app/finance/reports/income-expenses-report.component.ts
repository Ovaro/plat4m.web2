import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { DialogModule } from 'primeng/dialog';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexLegend,
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
import { TransactionTreeOption } from '../transactions/transactions.types';
import {
  ReportConfig,
  ReportDatePreset,
  ReportDefinition,
  ReportFilterOption,
  ReportResult,
  ReportRowDimension,
  ReportTreeOption,
  ReportView,
} from './reports.types';
import { ReportsService } from './reports.service';

type ViewOption = { label: string; value: ReportView };
type DateOption = { label: string; value: ReportDatePreset };
type ConfigTab = 'layout' | 'accounts' | 'categories' | 'payees' | 'family' | 'title' | 'view';

type AxisChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  stroke: ApexStroke;
  dataLabels: ApexDataLabels;
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

@Component({
  selector: 'jhi-income-expenses-report',
  templateUrl: './income-expenses-report.component.html',
  styleUrls: ['./income-expenses-report.component.scss'],
  imports: [SharedModule, DialogModule, NgApexchartsModule],
})
export class IncomeExpensesReportComponent implements OnInit {
  protected readonly reportKey = 'income-expenses';
  protected readonly viewOptions: ViewOption[] = [
    { label: 'Report', value: 'report' },
    { label: 'Bar Chart', value: 'bar' },
    { label: 'Line Chart', value: 'line' },
    { label: 'Pie Chart', value: 'pie' },
  ];
  protected readonly dateOptions: DateOption[] = [
    { label: '1M', value: '1M' },
    { label: '3M', value: '3M' },
    { label: '6M', value: '6M' },
    { label: '12M', value: '12M' },
    { label: 'YTD', value: 'YTD' },
    { label: 'ALL', value: 'ALL' },
    { label: 'Custom', value: 'custom' },
  ];
  protected readonly rowOptions: { label: string; value: ReportRowDimension }[] = [
    { label: 'Category Types', value: 'categoryType' },
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

  private readonly reportsService = inject(ReportsService);
  private readonly accountListService = inject(AccountList);
  private readonly manageDataService = inject(FinanceManageDataService);
  private readonly transactionsService = inject(Transactions);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

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

  get selectedConfigName(): string {
    return this.workingConfig?.builtin ? 'Default' : (this.workingConfig?.name ?? 'Default');
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
    this.runReport();
  }

  onViewChanged(option: ViewOption | null): void {
    if (!option || !this.workingConfig) {
      return;
    }
    this.workingConfig.defaultView = option.value;
    this.syncCharts();
  }

  onCustomDatesChanged(): void {
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
    this.runReport();
  }

  onCheckboxListToggle(listKey: 'accountIds' | 'categoryIds' | 'payeeIds' | 'familyMemberIds', id: string, checked: boolean): void {
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
    this.runReport();
  }

  selectAllCategories(): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig.categoryIds = this.categoryOptions.map(option => option.id);
    this.runReport();
  }

  selectIncomeCategories(): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig.categoryIds = this.categoryOptions.filter(option => option.rootType === 'Income').map(option => option.id);
    this.runReport();
  }

  selectExpenseCategories(): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig.categoryIds = this.categoryOptions.filter(option => option.rootType === 'Expense').map(option => option.id);
    this.runReport();
  }

  clearAllCategories(): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig.categoryIds = [];
    this.runReport();
  }

  selectAllList(listKey: 'accountIds' | 'payeeIds' | 'familyMemberIds'): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig[listKey] = this.getOptionsForList(listKey).map(option => option.id);
    this.runReport();
  }

  clearList(listKey: 'accountIds' | 'payeeIds' | 'familyMemberIds'): void {
    if (!this.workingConfig) {
      return;
    }
    this.workingConfig[listKey] = [];
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

    const payload = this.cloneConfig(this.workingConfig);
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

  isSelected(listKey: 'accountIds' | 'categoryIds' | 'payeeIds' | 'familyMemberIds', id: string): boolean {
    return !!this.workingConfig?.[listKey].includes(id);
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

    this.axisChartOptions = {
      series: (this.result?.series ?? []).map(series => ({ name: series.name, data: series.data })),
      chart: {
        type: this.workingConfig?.defaultView === 'line' ? 'line' : 'bar',
        toolbar: { show: false },
        height: 380,
      },
      stroke: {
        curve: 'smooth',
        width: this.workingConfig?.defaultView === 'line' ? 3 : 1,
      },
      dataLabels: { enabled: false },
      tooltip: { y: { formatter: value => this.formatCurrency(Number(value ?? 0)) } },
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
      series: (this.result?.pie ?? []).map(segment => segment.value),
      chart: { type: 'pie', height: 380 },
      labels: (this.result?.pie ?? []).map(segment => segment.label),
      legend: { position: 'bottom' },
      dataLabels: { enabled: true },
      tooltip: {
        y: {
          formatter: value => this.formatCurrency(Number(value ?? 0)),
        },
      },
      colors: palette,
    };
  }

  private getOptionsForList(listKey: 'accountIds' | 'payeeIds' | 'familyMemberIds'): ReportFilterOption[] {
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

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: this.result?.currencyCode ?? 'AUD',
      currencyDisplay: 'narrowSymbol',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value ?? 0);
  }

  private getErrorMessage(error: any, fallback: string): string {
    return error?.error?.detail || error?.error?.message || fallback;
  }
}
