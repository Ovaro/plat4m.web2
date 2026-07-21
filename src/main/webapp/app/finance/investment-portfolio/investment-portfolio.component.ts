import { AfterViewInit, ChangeDetectorRef, Component, Inject, LOCALE_ID, NgZone, OnDestroy, ViewChild, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ThemeChangeEvent, ThemeService } from 'app/layouts/main/theme.service';
import { InvestmentPortfolio } from './investment-portfolio.service';
import {
  CustomPortfolio,
  CustomPortfolioAccountOption,
  CustomPortfolioOptions,
  CustomPortfolioSecurityOption,
  CustomPortfolioStrategy,
  FinanceInvestmentSnapshotDetails,
  PortfolioTrade,
  FinanceResourceSnapshots,
  FinanceSecurityHolding,
  FinancialAccount,
  InvestmentPortfolioDetails,
  Periods,
  PortfolioValueHistoryItem,
} from '../finance.model';
import { formatCurrency, formatDate, getCurrencySymbol } from '@angular/common';
import { AccountList } from '../account-list/account-list.service';
//import { CommonControllerServices } from 'app/core/util/common-controller.service';
import { CookieService } from 'ngx-cookie';
import {
  ApexAnnotations,
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexLegend,
  ApexOptions,
  ApexPlotOptions,
  ApexTheme,
  ApexTitleSubtitle,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  ChartComponent,
  XAxisAnnotations,
  AnnotationLabel,
  NgApexchartsModule,
} from 'ng-apexcharts';
import SharedModule from 'app/shared/shared.module';
import { SkeletonModule } from 'primeng/skeleton';
import { DialogModule } from 'primeng/dialog';
import { DeltaValueComponent } from 'app/shared/delta-value/delta-value.component';
import { AlertService } from 'app/core/util/alert.service';
import { FinanceSecurityPriceRefreshItem, FinanceSecurityPriceRefreshResult } from '../finance.model';
import { InvestmentTradesComponent } from './investment-trades.component';

interface GroupData {
  [name: string]: FinanceSecurityHolding[] | null;
}

interface Group {
  name: string;
  value: string;
  groups: GroupData | null;
}

type HoldingSortColumn =
  | 'symbol'
  | 'name'
  | 'price'
  | 'periodChange'
  | 'quantity'
  | 'value'
  | 'capitalInvested'
  | 'capitalGain'
  | 'income'
  | 'currencyGain'
  | 'totalReturn';

interface AccountSelectGroup {
  label: string;
  items: PortfolioSelectOption[];
}

interface PortfolioSelectOption {
  id: string;
  name: string;
  type: 'account' | 'portfolio';
  sourceId: string | null;
}

interface InvestmentSelectOption {
  id: string;
  name: string;
  symbol: string;
  accountName: string;
  closed: boolean;
}

interface PieChartData {
  series: number[];
  labels: string[];
}

interface InvestmentPortfolioScreenState {
  selectedViewId?: string | null;
  accountId?: string | null;
  customPortfolioId?: string | null;
  includeClosedPositions?: boolean;
  showAnnotations?: boolean;
  selectedGroupValue?: string | null;
  selectedPeriodValue?: string | null;
  selectedInvestmentName?: string | null;
  activeTab?: string | null;
  holdingSortColumn?: HoldingSortColumn | null;
  holdingSortDirection?: 'asc' | 'desc' | null;
}

export type ChartOptions = {
  series: number[];
  labels: string[];
  chart: ApexChart;
  legend: ApexLegend;
  theme: ApexTheme;
  plotOptions: ApexPlotOptions;
  colors: string[];
  dataLabels: ApexDataLabels;
  options: ApexOptions;
  tooltip: ApexTooltip;
};

export type AreaChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  title: ApexTitleSubtitle;
  stacked: boolean;
  options: ApexOptions;
  tooltip: ApexTooltip;
  annotations: ApexAnnotations;
};

export type TreeMapChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  legend: ApexLegend;
  title: ApexTitleSubtitle;
  plotOptions: ApexPlotOptions;
  colors: string[];
  dataLabels: ApexDataLabels;
  tooltip: ApexTooltip;
  theme?: ApexTheme;
};

@Component({
  selector: 'jhi-investment-portfolio',
  templateUrl: './investment-portfolio.component.html',
  styleUrls: ['./investment-portfolio.component.scss'],
  imports: [SharedModule, RouterModule, NgApexchartsModule, SkeletonModule, DialogModule, InvestmentTradesComponent],
})
export class InvestmentPortfolioComponent implements AfterViewInit, OnDestroy {
  static COOKIE_SHOW_CLOSED = 'show-closed-state';
  static PERIOD_DEFAULT_COOKIE_ID = 'period-default';
  static SCREEN_STATE_COOKIE_ID = 'investment-portfolio-screen-state';

  static ALL_ACCOUNTS: FinancialAccount = {
    id: '',
    name: 'All Investment Accounts',
    type: -1,
    accountType: 'All',
    currencyCode: '',
    balance: 0,
    balanceWarning: '',
    fxDateTime: null,
    relatedToAccountId: null,
    closed: false,
    favourite: false,
    institution: null,
    startingBalance: 0,
    fxRateToLocal: null,
  };

  private readonly _title = signal('');
  private readonly _theme = signal('light');
  private readonly _isLoading = signal(false);
  private readonly _isLoadingSummaries = signal(false);
  private readonly _isLoadingHistory = signal(false);
  private readonly _isRefreshingQuotes = signal(false);
  private readonly _isHistoryLoaded = signal(false);
  private readonly _showHistoryChart = signal(false);
  private readonly _showPieChart = signal(true);
  private readonly _isApplyingQuoteRefreshResults = signal(false);

  private readonly _accountId = signal<string | null>(null);
  private readonly _account = signal<FinancialAccount | null>(null);
  private readonly _customPortfolioId = signal<string | null>(null);
  private readonly _selectedViewId = signal('account:');
  groupings: Group[];

  private readonly _includeClosedPositions = signal(false);

  private readonly _showAnnotations = signal(false);

  private readonly _selectedGroup = signal<Group>({ name: 'Holdings', value: 'holdings', groups: null });

  periods: Periods[];
  private readonly _selectedPeriod = signal<Periods>({ name: 'ALL', value: '' });

  private readonly _selectedInvestmentName = signal<string | null>(null);

  /* Tabs */
  mainTab: string;
  private readonly _activeTab = signal('holdings');

  // pipe = new DatePipe('en-US');

  steadyVariance = 0.01; // mark as steady if not more or less than this

  // Data that gets displayed in the grid
  private readonly _holdings = signal<FinanceSecurityHolding[] | null>(null);
  private readonly _investmentSelectOptions = signal<InvestmentSelectOption[]>([]);
  private readonly _selectedInvestmentId = signal<string | null>(null);

  private readonly _accounts = signal<FinancialAccount[] | null>(null);
  private readonly _customPortfolios = signal<CustomPortfolio[]>([]);
  customPortfolioOptions: CustomPortfolioOptions = { securities: [], accounts: [] };
  accountSelectOptions: AccountSelectGroup[] = [];
  portfolioDialogVisible = false;
  portfolioDialogMode: 'create' | 'edit' = 'create';
  portfolioForm: CustomPortfolio = this.emptyPortfolioForm();
  portfolioExpectedReturnPercent = 7.5;
  portfolioSaving = false;
  portfolioError: string | null = null;
  portfolioHideClosedAndZeroOptions = true;
  protected holdingSortColumn: HoldingSortColumn = 'name';
  protected holdingSortDirection: 'asc' | 'desc' = 'asc';
  readonly portfolioStrategyOptions: { label: string; value: CustomPortfolioStrategy }[] = [
    { label: 'Growth', value: 'GROWTH' },
    { label: 'Income', value: 'INCOME' },
    { label: 'Balanced', value: 'BALANCED' },
    { label: 'Defensive', value: 'DEFENSIVE' },
    { label: 'Speculative', value: 'SPECULATIVE' },
    { label: 'Thematic', value: 'THEMATIC' },
    { label: 'Index/Core', value: 'INDEX_CORE' },
    { label: 'Custom', value: 'CUSTOM' },
  ];

  private readonly _summaries = signal<FinanceInvestmentSnapshotDetails[] | null>(null);

  private readonly _portfolio = signal<InvestmentPortfolioDetails | null>(null);

  private readonly _portfolioValueHistoryItems = signal<FinanceResourceSnapshots[] | null>(null);

  private readonly _comparisonPortfolio = signal<InvestmentPortfolioDetails | null>(null); // The period to compare against
  private readonly _quoteRefreshMessage = signal<string | null>(null);
  private readonly _quoteRefreshDialogVisible = signal(false);
  private readonly _quoteRefreshResult = signal<FinanceSecurityPriceRefreshResult | null>(null);
  private readonly _isLoadingTrades = signal(false);
  private quoteRefreshPollHandle: ReturnType<typeof setTimeout> | null = null;
  private completedRefreshJobId: string | null = null;

  trades: PortfolioTrade[] = [];

  /* Charts */
  @ViewChild('pieChart', { static: false }) pieChart!: ChartComponent;
  public pieChartOptions: Partial<ChartOptions>;

  @ViewChild('treemapChart', { static: false }) treemapChart!: ChartComponent;
  public treemapChartOptions: Partial<TreeMapChartOptions>;

  @ViewChild('historyChart', { static: false }) historyChart!: ChartComponent;
  public historyChartOptions: Partial<AreaChartOptions>;

  readonly emptyTreemapSeries: ApexAxisChartSeries = [{ name: 'Holdings', data: [] }];
  readonly emptyHistorySeries: ApexAxisChartSeries = [{ name: 'All Holdings', data: [] }];

  get title(): string {
    return this._title();
  }

  set title(value: string) {
    this._title.set(value);
  }

  get theme(): string {
    return this._theme();
  }

  set theme(value: string) {
    this._theme.set(value);
  }

  get isLoading(): boolean {
    return this._isLoading();
  }

  set isLoading(value: boolean) {
    this._isLoading.set(value);
  }

  get isLoadingSummaries(): boolean {
    return this._isLoadingSummaries();
  }

  set isLoadingSummaries(value: boolean) {
    this._isLoadingSummaries.set(value);
  }

  get isLoadingHistory(): boolean {
    return this._isLoadingHistory();
  }

  set isLoadingHistory(value: boolean) {
    this._isLoadingHistory.set(value);
  }

  get isRefreshingQuotes(): boolean {
    return this._isRefreshingQuotes();
  }

  set isRefreshingQuotes(value: boolean) {
    this._isRefreshingQuotes.set(value);
  }

  get isHistoryLoaded(): boolean {
    return this._isHistoryLoaded();
  }

  set isHistoryLoaded(value: boolean) {
    this._isHistoryLoaded.set(value);
  }

  get showHistoryChart(): boolean {
    return this._showHistoryChart();
  }

  set showHistoryChart(value: boolean) {
    this._showHistoryChart.set(value);
  }

  get showPieChart(): boolean {
    return this._showPieChart();
  }

  set showPieChart(value: boolean) {
    this._showPieChart.set(value);
  }

  get isApplyingQuoteRefreshResults(): boolean {
    return this._isApplyingQuoteRefreshResults();
  }

  set isApplyingQuoteRefreshResults(value: boolean) {
    this._isApplyingQuoteRefreshResults.set(value);
  }

  get isLoadingTrades(): boolean {
    return this._isLoadingTrades();
  }

  set isLoadingTrades(value: boolean) {
    this._isLoadingTrades.set(value);
  }

  get accountId(): string | null {
    return this._accountId();
  }

  set accountId(value: string | null) {
    this._accountId.set(value);
  }

  get account(): FinancialAccount | null {
    return this._account();
  }

  set account(value: FinancialAccount | null) {
    this._account.set(value);
  }

  get customPortfolioId(): string | null {
    return this._customPortfolioId();
  }

  set customPortfolioId(value: string | null) {
    this._customPortfolioId.set(value);
  }

  get selectedViewId(): string {
    return this._selectedViewId();
  }

  set selectedViewId(value: string | null) {
    this._selectedViewId.set(value ?? 'account:');
  }

  get includeClosedPositions(): boolean {
    return this._includeClosedPositions();
  }

  set includeClosedPositions(value: boolean) {
    this._includeClosedPositions.set(value);
  }

  get showAnnotations(): boolean {
    return this._showAnnotations();
  }

  set showAnnotations(value: boolean) {
    this._showAnnotations.set(value);
  }

  get selectedGroup(): Group {
    return this._selectedGroup();
  }

  set selectedGroup(value: Group | null) {
    this._selectedGroup.set(value ?? this.groupings[0]);
  }

  get selectedPeriod(): Periods {
    return this._selectedPeriod();
  }

  set selectedPeriod(value: Periods | null) {
    this._selectedPeriod.set(value ?? this.periods[this.periods.length - 1]);
  }

  get selectedInvestmentName(): string | null {
    return this._selectedInvestmentName();
  }

  set selectedInvestmentName(value: string | null) {
    this._selectedInvestmentName.set(value);
  }

  get activeTab(): string {
    return this._activeTab();
  }

  set activeTab(value: string) {
    this._activeTab.set(value);
  }

  get holdings(): FinanceSecurityHolding[] | null {
    return this._holdings();
  }

  set holdings(value: FinanceSecurityHolding[] | null) {
    this._holdings.set(value);
  }

  get investmentSelectOptions(): InvestmentSelectOption[] {
    return this._investmentSelectOptions();
  }

  set investmentSelectOptions(value: InvestmentSelectOption[]) {
    this._investmentSelectOptions.set(value ?? []);
  }

  get selectedInvestmentId(): string | null {
    return this._selectedInvestmentId();
  }

  set selectedInvestmentId(value: string | null) {
    this._selectedInvestmentId.set(value);
  }

  get accounts(): FinancialAccount[] | null {
    return this._accounts();
  }

  set accounts(value: FinancialAccount[] | null) {
    this._accounts.set(value);
    this.accountSelectOptions = this.buildAccountSelectOptions(value, this.customPortfolios);
  }

  get customPortfolios(): CustomPortfolio[] {
    return this._customPortfolios();
  }

  set customPortfolios(value: CustomPortfolio[]) {
    this._customPortfolios.set(value ?? []);
    this.accountSelectOptions = this.buildAccountSelectOptions(this.accounts, value ?? []);
  }

  get summaries(): FinanceInvestmentSnapshotDetails[] | null {
    return this._summaries();
  }

  set summaries(value: FinanceInvestmentSnapshotDetails[] | null) {
    this._summaries.set(value);
  }

  get portfolio(): InvestmentPortfolioDetails | null {
    return this._portfolio();
  }

  set portfolio(value: InvestmentPortfolioDetails | null) {
    this._portfolio.set(value);
  }

  get portfolioValueHistoryItems(): FinanceResourceSnapshots[] | null {
    return this._portfolioValueHistoryItems();
  }

  set portfolioValueHistoryItems(value: FinanceResourceSnapshots[] | null) {
    this._portfolioValueHistoryItems.set(value);
  }

  get comparisonPortfolio(): InvestmentPortfolioDetails | null {
    return this._comparisonPortfolio();
  }

  set comparisonPortfolio(value: InvestmentPortfolioDetails | null) {
    this._comparisonPortfolio.set(value);
  }

  get quoteRefreshMessage(): string | null {
    return this._quoteRefreshMessage();
  }

  set quoteRefreshMessage(value: string | null) {
    this._quoteRefreshMessage.set(value);
  }

  get quoteRefreshDialogVisible(): boolean {
    return this._quoteRefreshDialogVisible();
  }

  set quoteRefreshDialogVisible(value: boolean) {
    this._quoteRefreshDialogVisible.set(value);
  }

  get quoteRefreshResult(): FinanceSecurityPriceRefreshResult | null {
    return this._quoteRefreshResult();
  }

  set quoteRefreshResult(value: FinanceSecurityPriceRefreshResult | null) {
    this._quoteRefreshResult.set(value);
  }

  get quoteRefreshDialogTitle(): string {
    const result = this.quoteRefreshResult;
    if (!result) {
      return 'Price Refresh Results';
    }

    if (!result.complete) {
      return `Refreshing prices: ${result.processedCount ?? 0} of ${result.requestedCount ?? 0} processed`;
    }

    const processedCount = result.requestedCount ?? 0;
    const refreshedCount = result.refreshedCount ?? 0;
    const skippedCount = result.skippedCount ?? 0;
    const failedCount = result.failedCount ?? 0;
    return `Processed ${processedCount} holding${processedCount === 1 ? '' : 's'}: ${refreshedCount} refreshed, ${skippedCount} skipped, ${failedCount} failed`;
  }

  get quoteRefreshItems(): FinanceSecurityPriceRefreshItem[] {
    return this.quoteRefreshResult?.items ?? [];
  }

  get quoteRefreshApplyLabel(): string {
    if (this.quoteRefreshResult?.applyRequested) {
      return this.quoteRefreshResult.complete ? 'Applied' : 'Apply As Found';
    }
    return 'Apply';
  }

  constructor(
    private investmentService: InvestmentPortfolio,
    private route: ActivatedRoute,
    private router: Router,
    private themeService: ThemeService,
    private alertService: AlertService,
    private accountService: AccountList,
    private cookieService: CookieService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef,
    @Inject(LOCALE_ID) private locale: string,
  ) {
    //, private commonControllerServices:CommonControllerServices
    this.groupings = [
      { name: 'Holdings', value: 'holdings', groups: null },
      { name: 'Account', value: 'portfolio', groups: null },
      { name: 'Type', value: 'type', groups: null },
      { name: 'Sector', value: 'sector', groups: null },
      { name: 'Industry', value: 'industry', groups: null },
      { name: 'Exchange', value: 'exchange', groups: null },
      { name: 'Currency', value: 'currency', groups: null },
      //{name: 'Owner', value: 'owner', groups: null}
    ];
    this.selectedGroup = this.groupings[0];

    this.periods = [
      { name: '1D', value: '1D' },
      { name: '1W', value: '1W' },
      { name: '1M', value: '1M' },
      { name: '1Q', value: '1Q' },
      { name: '1Y', value: '1Y' },
      { name: '3Y', value: '3Y' },
      { name: '5Y', value: '5Y' },
      { name: '10Y', value: '10Y' },
      { name: 'ALL', value: '' },
    ];
    this.selectedPeriod = this.periods[this.periods.length - 1];

    this.pieChartOptions = {
      series: [], //44, 55, 13, 33
      chart: {
        height: 180,
        width: 300,
        type: 'donut',
      },
      labels: [],
      legend: {
        show: false,
      },
      theme: {
        palette: 'palette3', // upto palette10
      },
      colors: ['#5EBFEA', '#91E5C4', '#4F6FD1', '#B7C4F0', '#8ED1F6', '#0074AB', '#58B9EB', '#351973', '#2EA0D6', '#0084BD', '#D9F1FF'],
      dataLabels: {
        enabled: false,
      },
      options: {
        stroke: {
          show: false,
        },
      },
    };

    this.treemapChartOptions = {
      series: this.emptyTreemapSeries,
      legend: {
        show: true,
      },
      title: {},
      colors: ['#5EBFEA', '#91E5C4', '#4F6FD1', '#B7C4F0', '#8ED1F6', '#0074AB', '#58B9EB', '#351973', '#2EA0D6', '#0084BD', '#D9F1FF'],
      plotOptions: {
        treemap: {
          enableShades: true,
          distributed: true,
        },
      },
    };

    this.historyChartOptions = {
      series: this.emptyHistorySeries,
      annotations: {},
    };

    this.mainTab = 'holdings';
    this.activeTab = 'holdings';
  }

  ngOnInit(): void {
    this.theme = this.themeService.theme();

    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;
      this.applyThemeToCharts();
    });

    //this.title = this.commonControllerServices.getPageTitle(this.router.routerState.snapshot.root);

    this.accountService.getByType(5).subscribe({
      next: (res: FinancialAccount[]) => {
        this.accounts = res;
        this.addAllOptionToAccounts();

        if (this.accountId != null) {
          this.account = this.getAccount(this.accountId);
        }
      },
      // error: () => (),
    });
    this.loadCustomPortfolios();
    this.loadCustomPortfolioOptions();

    const showClosedState = this.getCookie(InvestmentPortfolioComponent.COOKIE_SHOW_CLOSED);

    if (showClosedState !== undefined) {
      this.includeClosedPositions = showClosedState.toLowerCase() === 'true';
    }

    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const ptr = this;

    this.pieChartOptions.plotOptions = {
      pie: {
        donut: {
          // strokeWidth: 0,
          labels: {
            show: true,
            value: {
              fontSize: '12px',
              fontWeight: 'bold',
              offsetY: -10,
            },
            total: {
              show: true,
              //label: "TOTAL",
              //fontSize: "14px",
              showAlways: true,
              // eslint-disable-next-line object-shorthand
              formatter: function (w) {
                // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
                let s = 0;
                for (let i = 0; i < w.globals.series.length; i++) {
                  // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
                  s = s + w.globals.series[i];
                }
                return ptr.currencyFormatter(s);
              },
            },
          },
        },
      },
    };

    this.pieChartOptions.chart = {
      height: 200,
      width: 300,
      type: 'donut',
      events: {
        // eslint-disable-next-line object-shorthand
        dataPointSelection: function (event, chartContext, config) {
          // eslint-disable-next-line no-console
          console.log('event: ' + JSON.stringify(config.selectedDataPoints) + ' ' + JSON.stringify(config.seriesIndex));
          ptr.onPieClick(event, config.selectedDataPoints[0][0]);
        },
      },
    };

    ((this.treemapChartOptions.chart = {
      type: 'treemap',
      height: 600,
      events: {
        // eslint-disable-next-line object-shorthand
        click: function (event, chartContext, config) {
          ptr.navigateToInvestmentFromSeriesIndex(config.seriesIndex, config.dataPointIndex);
        },
      },
    }),
      (this.treemapChartOptions.dataLabels = {
        enabled: true,
        distributed: true,
        formatter: function (val: string, op: any): string {
          return `${val}\n${ptr.currencyFormatter(op.value)}`;
        },
      }));

    this.treemapChartOptions.tooltip = {
      enabled: true,
      y: {
        // eslint-disable-next-line object-shorthand
        formatter: function (val: number, ops: any): string {
          return ptr.currencyFormatter(val); //, ptr.currencyFormatter(ops.value)]; // + '<br>' + String(op.value)
        },
      },
    };

    this.pieChartOptions.tooltip = {
      enabled: true,
      custom: function ({ series, seriesIndex, w }): string {
        const values = Array.isArray(series) ? series : [];
        const value = typeof values[seriesIndex] === 'number' ? values[seriesIndex] : 0;
        const total = values.reduce((sum: number, item: number) => sum + item, 0);
        const percentage = total > 0 ? (value / total) * 100 : 0;
        const label = Array.isArray(w?.globals?.labels) ? (w.globals.labels[seriesIndex] ?? '') : '';

        return `
          <div
            class="apexcharts-tooltip-box"
            style="
              background: var(--ps-surface-raised, #ffffff);
              color: var(--ps-text-primary, #0f172a);
              border: 1px solid var(--ps-surface-border-subtle, rgba(148, 163, 184, 0.35));
              border-radius: 0.75rem;
              box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
              padding: 0.5rem 0.75rem;
            "
          >
            <div class="apexcharts-tooltip-title" style="margin-bottom: 0.35rem; color: var(--ps-text-primary, #0f172a);"><b>${label}</b></div>
            <div class="apexcharts-tooltip-series-group apexcharts-active tooltip-series-group" style="display: flex;">
              <span class="apexcharts-tooltip-marker" style="background-color: ${w.globals.colors[seriesIndex]}"></span>
              <div class="apexcharts-tooltip-text" style="color: var(--ps-text-primary, #0f172a);">
                <div class="apexcharts-tooltip-y-group" style="color: var(--ps-text-primary, #0f172a);">
                  <span class="apexcharts-tooltip-text-y-value" style="color: var(--ps-text-primary, #0f172a);">${ptr.currencyFormatter(value)} (${percentage.toFixed(2)}%)</span>
                </div>
              </div>
            </div>
          </div>
        `;
      },
    };

    this.historyChartOptions = {
      series: [], //44, 55, 13, 33
      chart: {
        height: 300,
        type: 'area',
        stacked: true,

        animations: {
          enabled: false,
          dynamicAnimation: {
            enabled: true,
          },
        },
      },
      options: {
        stroke: {
          show: false,
          curve: 'straight',
        },
        markers: {
          size: 1,
          strokeOpacity: 0.1,
          fillOpacity: 0.1,
          hover: {
            size: 4,
            sizeOffset: 3,
          },
        },
        dataLabels: {
          enabled: false,
        },
        theme: { mode: 'light' },
        tooltip: {
          shared: false,
          intersect: true,
          theme: this.theme,
          custom: ({ series, seriesIndex, dataPointIndex, w }) => {
            //  // eslint-disable-next-line
            //  console.info("hover[start]-sI:" + seriesIndex + ", dpI:" + dataPointIndex + ", w:" + JSON.stringify(w.globals.colors));
            const hoverXaxis = w.globals.seriesX[seriesIndex][dataPointIndex];

            const hoverIndexes = w.globals.seriesX.map((seriesX: any[]) => {
              return seriesX.findIndex(xData => xData === hoverXaxis);
            });

            let hoverList = '';
            let sum = 0;

            hoverIndexes.forEach((hoverIndex: number, seriesEachIndex: string | number) => {
              if (hoverIndex >= 0) {
                // if(seriesEachIndex === seriesIndex) {
                //   hoverList += `<span style="background-color: lightgray">`;
                // }
                hoverList += `<div class="apexcharts-tooltip-series-group apexcharts-active tooltip-series-group" style="order: 1; display: flex"><span class="apexcharts-tooltip-marker" style="background-color: ${w.globals.colors[seriesEachIndex]}"></span><div class="apexcharts-tooltip-text"><div class="apexcharts-tooltip-y-group">`;
                if (seriesEachIndex !== seriesIndex) {
                  hoverList += `<span class="apexcharts-tooltip-text-y-label">${w.globals.seriesNames[seriesEachIndex]}:</span> `;
                } else {
                  hoverList += `<span class="apexcharts-tooltip-text-y-label"><u>${w.globals.seriesNames[seriesEachIndex]}</u>:</span> `;
                }
                hoverList +=
                  '<span class="apexcharts-tooltip-text-y-value">' +
                  this.currencyFormatter(series[seriesEachIndex][hoverIndex]) +
                  '</span>';

                hoverList += `</div></div></div>`;
                sum = sum + series[seriesEachIndex][hoverIndex];
                // if(seriesEachIndex === seriesIndex) {
                //   hoverList += `</span>`;
                // }
              }
            });
            //const formatHoverX = this.dateFormat(new Date(hoverXaxis), 'YYYY-MM-DD HH:mm:ss');
            const format = 'dd/MM/yyyy';
            let date = formatDate(hoverXaxis, format, this.locale);
            //let date = this.datePipe.transform(hoverXaxis);
            let sumVal = this.currencyFormatter(sum);
            //  ${s} <div class="card"> </div>
            //
            return `
                <div class="apexcharts-tooltip-title"><b>${date} (${sumVal})</b></div>
                ${hoverList}`;
          },
        },
      },
      xaxis: {
        type: 'datetime',
      },
      yaxis: {
        labels: {
          formatter: function (val) {
            // eslint-disable-next-line
            return ptr.currencyFormatter(val);
            //return "$"+(val).toFixed(2);
          },
        },
      },
      annotations: {
        //position: 'back' //- this stopped working when upgrade of ApexCharts....
      },
    };

    const savedPeriodDefault = this.getCookie(InvestmentPortfolioComponent.PERIOD_DEFAULT_COOKIE_ID);
    if (savedPeriodDefault) {
      for (let i = 0; i < this.periods.length; i++) {
        if (this.periods[i].value === savedPeriodDefault) {
          this.selectedPeriod = this.periods[i];
        }
      }
    }
    this.restoreScreenState();

    this.route.params.subscribe(params => {
      if (params['id']) {
        this.accountId = params['id'];
        this.selectedViewId = `account:${params['id']}`;
        this.customPortfolioId = null;
        this.account = this.accountId ? this.getAccount(this.accountId) : null;
        this.saveScreenState();
      }
    });

    this.resumeActiveQuoteRefresh();
    this.loadInvestmentSelectOptions();
    this.load();
    this.applyThemeToCharts();
  }

  ngAfterViewInit(): void {
    this.applyThemeToCharts();
    this.syncPieChart();
  }

  ngOnDestroy(): void {
    this.clearQuoteRefreshPoll();
  }

  navigateToInvestmentFromSeriesIndex(series: number, index: number): void {
    const selectedSeries = this.treemapChartOptions.series?.[series];
    const dp = selectedSeries?.data?.[index] as { x: any; y: any } | undefined;

    if (!dp) {
      return;
    }

    for (let i = 0; i < this.holdings!.length; i++) {
      if (this.holdings![i].name === dp.x) {
        // assume this is the right one
        this.navigateToInvestment(this.holdings![i].id);
        break;
      }
    }
  }

  navigateToInvestment(id: string): void {
    this.zone.run(() => {
      this.router.navigate(['/', 'investment', id], { skipLocationChange: false }).then(
        nav => {
          // eslint-disable-next-line no-console
          console.log(nav); // true if navigation is successful
        },
        err => {
          // eslint-disable-next-line no-console
          console.log(err); // when there's an error
        },
      );
    });
  }

  onChangeAccount(event: any): void {
    const selectedValue = (event?.value as string | null) ?? this.selectedViewId;
    this.applySelectedView(selectedValue);
    this.saveScreenState();
    this.load();
    this.clearGroupCache();
  }

  onChangeInvestment(event: any): void {
    const selectedInvestmentId = (event?.value as string | null) ?? this.selectedInvestmentId;
    if (!selectedInvestmentId) {
      return;
    }
    this.navigateToInvestment(selectedInvestmentId);
  }

  onChangeGrouping(event: any): void {
    const nextGroup = event?.value as Group | null | undefined;
    if (nextGroup) {
      this.selectedGroup = nextGroup;
      this.saveScreenState();
    }

    if (this.holdings && this.selectedGroup) {
      this.selectedInvestmentName = null;
      this.processGroup(this.selectedGroup, this.holdings);
      if (this.isHistoryLoaded && this.selectedGroup.groups && this.portfolioValueHistoryItems) {
        this.setHistorySeries(this.configureHistorySeriesByGroup(this.selectedGroup.groups, this.portfolioValueHistoryItems));
        this.setHistoryAnnotations(this.showAnnotations ? this.processAnnotations(this.portfolioValueHistoryItems) : {});
        this.refreshHistoryChart();
      }
    }
  }

  onChangePeriod(event: any): void {
    const nextPeriod = event?.value as Periods | null | undefined;
    if (nextPeriod) {
      this.selectedPeriod = nextPeriod;
    }

    this.zone.run(() => {
      this.comparisonPortfolio = null;
      this.saveCookie(InvestmentPortfolioComponent.PERIOD_DEFAULT_COOKIE_ID, this.selectedPeriod.value);
      this.saveScreenState();
      if (this.holdings) {
        //this.processPeriod(this.selectedGroup, this.holdings);

        //this.selectedPeriod.value
        let acId = this.accountId;
        if (acId === '') {
          acId = null;
        }
        this.loadComparisons(acId!);
        this.loadHistory();
      }
    });
  }

  loadComparisons(acId: string | null) {
    this.investmentService.getSummaries(acId, true, this.selectedPeriod.value, this.customPortfolioId).subscribe({
      next: (res: InvestmentPortfolioDetails) => {
        this.comparisonPortfolio = res;
      },
      error: () => (this.isLoadingSummaries = false),
    });
  }

  clickTab(tabName: string): void {
    // // console.log('activetab pre: ' + this.activetab);
    this.activeTab = tabName;
    this.saveScreenState();
    if (this.activeTab === 'treemap') {
      this.setTreemapSeries(this.processTreemapData());
    } else if (this.activeTab === 'history') {
      //this.historyChartOptions.series = this.processTreemapData();
      //this.loadHistory();
    }
  }

  onPieClick(event: any, selectedDataPoint: number): void {
    // eslint-disable-next-line no-console
    console.log('event: ' + JSON.stringify(selectedDataPoint));

    this.zone.run(() => {
      this.selectedInvestmentName = this.pieChartOptions.labels![selectedDataPoint];
      this.saveScreenState();
    });
  }

  getAccount(id: string): FinancialAccount | null {
    if (id === '') {
      return null;
    }
    let val = null;
    this.accounts?.every(element => {
      if (element.id === id) {
        val = element;
        return false;
      }
      return true;
    });
    return val;
  }

  selectedCustomPortfolio(): CustomPortfolio | null {
    if (!this.customPortfolioId) {
      return null;
    }
    return this.customPortfolios.find(portfolio => portfolio.id === this.customPortfolioId) ?? null;
  }

  private applySelectedView(selectedValue: string): void {
    this.selectedViewId = selectedValue;
    if (selectedValue.startsWith('portfolio:')) {
      this.customPortfolioId = selectedValue.replace('portfolio:', '') || null;
      this.accountId = null;
      this.account = null;
      return;
    }

    const accountId = selectedValue.replace('account:', '');
    this.customPortfolioId = null;
    this.accountId = accountId;
    this.account = this.getAccount(accountId);
  }

  private loadCustomPortfolios(): void {
    this.investmentService.getCustomPortfolios().subscribe({
      next: portfolios => {
        this.customPortfolios = portfolios ?? [];
        if (this.customPortfolioId && !this.customPortfolios.some(portfolio => portfolio.id === this.customPortfolioId)) {
          this.applySelectedView('account:');
          this.saveScreenState();
        }
      },
    });
  }

  private loadCustomPortfolioOptions(): void {
    this.investmentService.getCustomPortfolioOptions().subscribe({
      next: options => {
        this.customPortfolioOptions = options ?? { securities: [], accounts: [] };
      },
    });
  }

  openCreatePortfolioDialog(): void {
    this.portfolioDialogMode = 'create';
    this.portfolioForm = this.emptyPortfolioForm();
    this.portfolioExpectedReturnPercent = 7.5;
    this.portfolioHideClosedAndZeroOptions = true;
    this.portfolioError = null;
    this.portfolioDialogVisible = true;
  }

  openEditPortfolioDialog(): void {
    const portfolio = this.selectedCustomPortfolio();
    if (!portfolio) {
      return;
    }
    this.portfolioDialogMode = 'edit';
    this.portfolioForm = {
      ...portfolio,
      securityIds: [...(portfolio.securityIds ?? [])],
      accountIds: [...(portfolio.accountIds ?? [])],
    };
    this.portfolioExpectedReturnPercent = Number(((portfolio.expectedReturnCagr ?? 0.075) * 100).toFixed(4));
    this.portfolioHideClosedAndZeroOptions = true;
    this.portfolioError = null;
    this.portfolioDialogVisible = true;
  }

  savePortfolio(): void {
    this.portfolioError = null;
    if (!this.portfolioForm.name || this.portfolioForm.name.trim().length === 0) {
      this.portfolioError = 'Portfolio name is required.';
      return;
    }
    if (
      this.portfolioForm.strategy === 'CUSTOM' &&
      (!this.portfolioForm.customStrategy || this.portfolioForm.customStrategy.trim().length === 0)
    ) {
      this.portfolioError = 'Custom strategy is required.';
      return;
    }

    const payload: CustomPortfolio = {
      ...this.portfolioForm,
      name: this.portfolioForm.name.trim(),
      description: this.portfolioForm.description?.trim() || null,
      customStrategy: this.portfolioForm.strategy === 'CUSTOM' ? this.portfolioForm.customStrategy?.trim() || null : null,
      expectedReturnCagr: Number(this.portfolioExpectedReturnPercent || 0) / 100,
      securityIds: [...(this.portfolioForm.securityIds ?? [])],
      accountIds: [...(this.portfolioForm.accountIds ?? [])],
    };

    this.portfolioSaving = true;
    const request =
      this.portfolioDialogMode === 'edit' && payload.id
        ? this.investmentService.updateCustomPortfolio(payload)
        : this.investmentService.createCustomPortfolio(payload);
    request.subscribe({
      next: saved => {
        this.portfolioSaving = false;
        this.portfolioDialogVisible = false;
        this.loadCustomPortfolios();
        this.loadCustomPortfolioOptions();
        this.applySelectedView(`portfolio:${saved.id}`);
        this.saveScreenState();
        this.clearGroupCache();
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.portfolioSaving = false;
        this.portfolioError = this.getQuoteRefreshErrorMessage(error);
      },
    });
  }

  deleteSelectedPortfolio(): void {
    const portfolio = this.selectedCustomPortfolio();
    if (!portfolio?.id) {
      return;
    }
    this.portfolioSaving = true;
    this.investmentService.deleteCustomPortfolio(portfolio.id).subscribe({
      next: () => {
        this.portfolioSaving = false;
        this.portfolioDialogVisible = false;
        this.applySelectedView('account:');
        this.saveScreenState();
        this.loadCustomPortfolios();
        this.clearGroupCache();
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.portfolioSaving = false;
        this.portfolioError = this.getQuoteRefreshErrorMessage(error);
      },
    });
  }

  onStrategyChange(): void {
    if (this.portfolioForm.strategy !== 'CUSTOM') {
      this.portfolioForm.customStrategy = null;
    }
  }

  isSecuritySelected(security: CustomPortfolioSecurityOption): boolean {
    return this.portfolioForm.securityIds?.includes(security.id) ?? false;
  }

  filteredPortfolioSecurities(): CustomPortfolioSecurityOption[] {
    if (!this.portfolioHideClosedAndZeroOptions) {
      return this.customPortfolioOptions.securities;
    }
    return this.customPortfolioOptions.securities.filter(
      security => this.isSecuritySelected(security) || Number(security.currentQuantity ?? 0) > 0,
    );
  }

  toggleSecurity(security: CustomPortfolioSecurityOption, selected: boolean): void {
    this.portfolioForm.securityIds = this.toggleId(this.portfolioForm.securityIds, security.id, selected);
  }

  isCashAccountSelected(account: CustomPortfolioAccountOption): boolean {
    return this.portfolioForm.accountIds?.includes(account.id) ?? false;
  }

  filteredPortfolioCashAccounts(): CustomPortfolioAccountOption[] {
    if (!this.portfolioHideClosedAndZeroOptions) {
      return this.customPortfolioOptions.accounts;
    }
    return this.customPortfolioOptions.accounts.filter(
      account => this.isCashAccountSelected(account) || (!account.closed && Number(account.currentBalance ?? 0) !== 0),
    );
  }

  toggleCashAccount(account: CustomPortfolioAccountOption, selected: boolean): void {
    this.portfolioForm.accountIds = this.toggleId(this.portfolioForm.accountIds, account.id, selected);
  }

  isCashHolding(holding: FinanceSecurityHolding): boolean {
    return holding.positionType === 'cash';
  }

  holdingDisplaySymbol(holding: FinanceSecurityHolding): string {
    return this.isCashHolding(holding) ? 'Cash' : holding.userSymbol || holding.symbol;
  }

  holdingDisplayType(holding: FinanceSecurityHolding): string {
    return this.isCashHolding(holding) ? 'Cash' : holding.typeName;
  }

  sortHoldings(column: HoldingSortColumn): void {
    if (this.holdingSortColumn === column) {
      this.holdingSortDirection = this.holdingSortDirection === 'asc' ? 'desc' : 'asc';
      this.saveScreenState();
      return;
    }
    this.holdingSortColumn = column;
    this.holdingSortDirection = this.defaultHoldingSortDirection(column);
    this.saveScreenState();
  }

  holdingSortIndicator(column: HoldingSortColumn): string {
    if (this.holdingSortColumn !== column) {
      return '';
    }
    return this.holdingSortDirection === 'asc' ? '^' : 'v';
  }

  sortedHoldings(holdings: FinanceSecurityHolding[] | null): FinanceSecurityHolding[] {
    const direction = this.holdingSortDirection === 'asc' ? 1 : -1;
    return [...(holdings ?? [])].sort((left, right) => this.compareHoldingValues(left, right) * direction);
  }

  private defaultHoldingSortDirection(column: HoldingSortColumn): 'asc' | 'desc' {
    return ['periodChange', 'quantity', 'value', 'capitalInvested', 'capitalGain', 'income', 'currencyGain', 'totalReturn'].includes(column)
      ? 'desc'
      : 'asc';
  }

  private compareHoldingValues(left: FinanceSecurityHolding, right: FinanceSecurityHolding): number {
    const leftValue = this.getHoldingSortValue(left);
    const rightValue = this.getHoldingSortValue(right);

    if (leftValue == null && rightValue == null) {
      return this.holdingDisplaySymbol(left).localeCompare(this.holdingDisplaySymbol(right));
    }
    if (leftValue == null) {
      return 1;
    }
    if (rightValue == null) {
      return -1;
    }
    if (typeof leftValue === 'number' && typeof rightValue === 'number') {
      const difference = leftValue - rightValue;
      return difference === 0 ? this.holdingDisplaySymbol(left).localeCompare(this.holdingDisplaySymbol(right)) : difference;
    }
    return String(leftValue).localeCompare(String(rightValue), undefined, { numeric: true, sensitivity: 'base' });
  }

  private getHoldingSortValue(holding: FinanceSecurityHolding): string | number | null {
    if (this.holdingSortColumn === 'symbol') {
      return this.holdingDisplaySymbol(holding);
    }
    if (this.holdingSortColumn === 'name') {
      return holding.name;
    }
    if (this.holdingSortColumn === 'price') {
      return this.isCashHolding(holding) ? null : (holding.price ?? null);
    }
    if (this.holdingSortColumn === 'periodChange') {
      return this.holdingPeriodChange(holding);
    }
    if (this.holdingSortColumn === 'quantity') {
      return this.isCashHolding(holding) ? null : (holding.quantity ?? null);
    }
    if (this.holdingSortColumn === 'value') {
      return this.getHoldingLocalValue(holding);
    }
    if (this.holdingSortColumn === 'capitalInvested') {
      return this.holdingCapitalInvested(holding);
    }
    if (this.holdingSortColumn === 'capitalGain') {
      return this.holdingCapitalGain(holding);
    }
    if (this.holdingSortColumn === 'income') {
      return this.holdingIncome(holding);
    }
    if (this.holdingSortColumn === 'currencyGain') {
      return this.holdingCurrencyGain(holding);
    }
    return this.holdingTotalReturn(holding);
  }

  private holdingPeriodChange(holding: FinanceSecurityHolding): number | null {
    if (this.isCashHolding(holding)) {
      return holding.cashReturnPercent ?? null;
    }
    const comparisonPrice = this.findComparison(holding.id)?.price;
    if (comparisonPrice == null || comparisonPrice === 0 || holding.price == null) {
      return null;
    }
    return (holding.price - comparisonPrice) / comparisonPrice;
  }

  private isHoldingSortColumn(value: unknown): value is HoldingSortColumn {
    return (
      typeof value === 'string' &&
      [
        'symbol',
        'name',
        'price',
        'periodChange',
        'quantity',
        'value',
        'capitalInvested',
        'capitalGain',
        'income',
        'currencyGain',
        'totalReturn',
      ].includes(value)
    );
  }

  private emptyPortfolioForm(): CustomPortfolio {
    return {
      id: null,
      name: '',
      description: null,
      strategy: 'BALANCED',
      customStrategy: null,
      expectedReturnCagr: 0.075,
      securityIds: [],
      accountIds: [],
    };
  }

  private toggleId(ids: string[] | null | undefined, id: string, selected: boolean): string[] {
    const current = new Set(ids ?? []);
    if (selected) {
      current.add(id);
    } else {
      current.delete(id);
    }
    return [...current];
  }

  currencyFormatterMethod(element: any): string {
    return this.currencyFormatter(element.value);
  }

  currencyFormatter(value: any): string {
    let code: string | undefined;
    if (this.account !== null) {
      code = this.account.currencyCode;
    }
    if (code === undefined) {
      // TODO Fix hard coding here
      code = 'AUD';
    }

    //const val = this.currencyPipe.transform( value, code, 'symbol-narrow');
    const val = formatCurrency(value, 'en-AU', '$', 'AUD');
    if (val === null) {
      // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
      return '' + value;
    }

    return val;
  }

  adjustedBalanceForCurrency(balance: number): number {
    // TODO: Fix hard coding of local currency here
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    if (this.account?.currencyCode !== 'AUD' && this.account?.fxRateToLocal != null) {
      // Convert
      return balance * this.account.fxRateToLocal;
    }

    return balance;
  }

  load(): void {
    this.isLoading = true;
    let acId = this.accountId;
    if (acId === '') {
      acId = null;
    }
    this.loadTrades(acId);
    this.investmentService.get(acId, this.includeClosedPositions, this.customPortfolioId, this.getPeriodString()).subscribe({
      next: (res: FinanceSecurityHolding[]) => {
        this.isLoading = false;
        this.holdings = res;

        this.processGroup(this.selectedGroup, this.holdings);
        // // eslint-disable-next-line no-console
        // console.log("selectedGroup: " + String(this.selectedGroup));
        // this.processPieChartData();
        //
        this.loadSummaries();

        this.loadHistory();
      },
      error: () => (this.isLoading = false),
    });
  }

  private loadInvestmentSelectOptions(): void {
    this.investmentService.get(null, true, null, '').subscribe({
      next: holdings => {
        this.investmentSelectOptions = this.buildInvestmentSelectOptions(holdings ?? []);
      },
      error: () => {
        this.investmentSelectOptions = [];
      },
    });
  }

  loadTrades(accountId: string | null): void {
    this.isLoadingTrades = true;
    this.investmentService.getTrades(accountId, this.includeClosedPositions, this.customPortfolioId).subscribe({
      next: (res: PortfolioTrade[]) => {
        this.isLoadingTrades = false;
        this.trades = res ?? [];
      },
      error: () => {
        this.isLoadingTrades = false;
        this.trades = [];
      },
    });
  }

  onChangePositionsSwitch(event: any): void {
    this.saveCookie(InvestmentPortfolioComponent.COOKIE_SHOW_CLOSED, this.includeClosedPositions);
    this.saveScreenState();
    this.load();
  }

  refreshQuotes(): void {
    const activeRefreshResult = this.investmentService.getActiveQuoteRefreshResult();
    if (activeRefreshResult && !activeRefreshResult.complete && activeRefreshResult.jobId) {
      this.isRefreshingQuotes = true;
      this.quoteRefreshResult = activeRefreshResult;
      this.quoteRefreshDialogVisible = true;
      this.scheduleQuoteRefreshPoll(activeRefreshResult.jobId);
      this.investmentService.getRefreshQuoteStatus(activeRefreshResult.jobId).subscribe({
        next: result => {
          this.handleQuoteRefreshResult(result);
          this.cdr.markForCheck();
        },
      });
      return;
    }

    if (this.isRefreshingQuotes) {
      this.quoteRefreshDialogVisible = true;
      const activeJobId = this.quoteRefreshResult?.jobId;
      if (activeJobId && !this.quoteRefreshResult?.complete) {
        this.investmentService.getRefreshQuoteStatus(activeJobId).subscribe({
          next: result => {
            this.handleQuoteRefreshResult(result);
            this.cdr.markForCheck();
          },
        });
      }
      return;
    }

    let acId = this.accountId;
    if (acId === '') {
      acId = null;
    }

    this.isRefreshingQuotes = true;
    this.quoteRefreshMessage = null;
    this.quoteRefreshResult = null;
    this.quoteRefreshDialogVisible = false;
    this.investmentService.refreshQuotes(acId).subscribe({
      next: result => {
        this.handleQuoteRefreshStarted(result);
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.isRefreshingQuotes = false;
        this.quoteRefreshMessage = this.getQuoteRefreshErrorMessage(error);
        this.cdr.markForCheck();
      },
    });
  }

  private handleQuoteRefreshStarted(result: FinanceSecurityPriceRefreshResult): void {
    this.isRefreshingQuotes = !result.complete;
    this.isApplyingQuoteRefreshResults = false;
    this.quoteRefreshResult = result;
    this.investmentService.setActiveQuoteRefreshResult(result);
    this.quoteRefreshDialogVisible = true;
    this.quoteRefreshMessage = null;
    this.completedRefreshJobId = null;
    if (result.jobId) {
      this.scheduleQuoteRefreshPoll(result.jobId);
    }
  }

  private handleQuoteRefreshResult(result: FinanceSecurityPriceRefreshResult): void {
    this.isRefreshingQuotes = !result.complete;
    this.isApplyingQuoteRefreshResults = false;
    this.quoteRefreshResult = result;
    this.investmentService.setActiveQuoteRefreshResult(result);
    if (!result.complete || !result.jobId) {
      return;
    }

    this.clearQuoteRefreshPoll();
    this.investmentService.clearActiveQuoteRefreshResult();
    if (this.completedRefreshJobId === result.jobId) {
      return;
    }

    this.completedRefreshJobId = result.jobId;
    this.load();
    this.alertService.addAlert({
      type: result.status === 'completed' ? 'success' : 'warning',
      message:
        result.status === 'completed'
          ? `Quote refresh completed: ${result.refreshedCount} refreshed, ${result.skippedCount} skipped, ${result.failedCount} failed.`
          : `Quote refresh ended with issues: ${result.refreshedCount} refreshed, ${result.skippedCount} skipped, ${result.failedCount} failed.`,
      toast: true,
    });
  }

  private scheduleQuoteRefreshPoll(jobId: string): void {
    this.clearQuoteRefreshPoll();
    this.quoteRefreshPollHandle = setTimeout(() => {
      this.investmentService.getRefreshQuoteStatus(jobId).subscribe({
        next: result => {
          this.handleQuoteRefreshResult(result);
          if (!result.complete) {
            this.scheduleQuoteRefreshPoll(jobId);
          }
          this.cdr.markForCheck();
        },
        error: () => {
          this.isRefreshingQuotes = false;
          this.investmentService.clearActiveQuoteRefreshResult();
          this.clearQuoteRefreshPoll();
        },
      });
    }, 2000);
  }

  private clearQuoteRefreshPoll(): void {
    if (this.quoteRefreshPollHandle !== null) {
      clearTimeout(this.quoteRefreshPollHandle);
      this.quoteRefreshPollHandle = null;
    }
  }

  private resumeActiveQuoteRefresh(): void {
    const activeRefreshResult = this.investmentService.getActiveQuoteRefreshResult();
    if (!activeRefreshResult || activeRefreshResult.complete || !activeRefreshResult.jobId) {
      return;
    }

    this.isRefreshingQuotes = true;
    this.quoteRefreshResult = activeRefreshResult;
    this.scheduleQuoteRefreshPoll(activeRefreshResult.jobId);
    this.investmentService.getRefreshQuoteStatus(activeRefreshResult.jobId).subscribe({
      next: result => {
        this.handleQuoteRefreshResult(result);
        this.cdr.markForCheck();
      },
      error: () => {
        this.isRefreshingQuotes = false;
        this.investmentService.clearActiveQuoteRefreshResult();
        this.cdr.markForCheck();
      },
    });
  }

  protected quoteRefreshStatusClass(status: string | null | undefined): string {
    switch (status) {
      case 'refreshed':
        return 'refresh-status refresh-status--success';
      case 'failed':
        return 'refresh-status refresh-status--failed';
      case 'pending':
        return 'refresh-status';
      default:
        return 'refresh-status refresh-status--skipped';
    }
  }

  protected formatRefreshCurrency(value: number | null | undefined, currencyCode: string | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }
    const resolvedCurrencyCode = currencyCode ?? 'AUD';
    return formatCurrency(value, this.locale, getCurrencySymbol(resolvedCurrencyCode, 'narrow'), resolvedCurrencyCode);
  }

  protected formatRefreshPercent(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }
    return `${(value / 100).toLocaleString(this.locale, { style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  private getQuoteRefreshErrorMessage(error: HttpErrorResponse): string {
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      return error.error;
    }

    if (error.error && typeof error.error === 'object') {
      if (typeof error.error.detail === 'string' && error.error.detail.trim().length > 0) {
        return error.error.detail;
      }
      if (typeof error.error.message === 'string' && error.error.message.trim().length > 0) {
        return error.error.message;
      }
      if (typeof error.error.title === 'string' && error.error.title.trim().length > 0) {
        return error.error.title;
      }
    }

    if (error.message && error.message.trim().length > 0) {
      return error.message;
    }

    return 'Refreshing quotes failed.';
  }

  protected canApplyQuoteRefreshResults(): boolean {
    const result = this.quoteRefreshResult;
    if (!result || !result.jobId || this.isApplyingQuoteRefreshResults) {
      return false;
    }
    return result.items.some(item => item.selected && item.canApply && !item.applied);
  }

  protected onQuoteRefreshSelectionChanged(item: FinanceSecurityPriceRefreshItem, selected: boolean): void {
    const result = this.quoteRefreshResult;
    if (!result?.jobId || !item.userSecurityId) {
      return;
    }

    item.selected = selected;
    if (!selected && !item.applied) {
      item.canApply = false;
      item.status = item.status === 'pending' ? 'skipped' : item.status;
      item.message = 'Skipped by user.';
    }
    this.investmentService.setActiveQuoteRefreshResult(result);
    this.investmentService.updateRefreshQuoteSelection(result.jobId, item.userSecurityId, selected).subscribe({
      next: updatedResult => {
        this.handleQuoteRefreshResult(updatedResult);
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.quoteRefreshMessage = this.getQuoteRefreshErrorMessage(error);
        this.investmentService.getRefreshQuoteStatus(result.jobId!).subscribe({
          next: refreshedResult => {
            this.handleQuoteRefreshResult(refreshedResult);
            this.cdr.markForCheck();
          },
        });
      },
    });
  }

  protected applyQuoteRefreshResults(): void {
    const jobId = this.quoteRefreshResult?.jobId;
    if (!jobId || this.isApplyingQuoteRefreshResults) {
      return;
    }

    this.isApplyingQuoteRefreshResults = true;
    this.investmentService.applyRefreshQuotes(jobId).subscribe({
      next: result => {
        this.handleQuoteRefreshResult(result);
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.isApplyingQuoteRefreshResults = false;
        this.quoteRefreshMessage = this.getQuoteRefreshErrorMessage(error);
        this.cdr.markForCheck();
      },
    });
  }

  onChangeAnnotationsSwitch(event: any): void {
    this.saveScreenState();
    if (this.showAnnotations) {
      this.historyChartOptions.annotations = this.processAnnotations(this.portfolioValueHistoryItems!);
    } else {
      this.historyChartOptions.annotations = {};
    }
  }

  addAllOptionToAccounts(): void {
    if (this.accounts != null) {
      this.accounts = [InvestmentPortfolioComponent.ALL_ACCOUNTS, ...this.accounts];
    }
  }

  private buildAccountSelectOptions(accounts: FinancialAccount[] | null, portfolios: CustomPortfolio[]): AccountSelectGroup[] {
    const accountItems = (accounts ?? []).map(account => ({
      id: `account:${account.id ?? ''}`,
      name: account.name,
      type: 'account' as const,
      sourceId: account.id || null,
    }));
    const groups: AccountSelectGroup[] = [
      {
        label: 'Investment Accounts',
        items: accountItems,
      },
    ];
    if (portfolios.length > 0) {
      groups.push({
        label: 'Custom Portfolios',
        items: portfolios.map(portfolio => ({
          id: `portfolio:${portfolio.id}`,
          name: portfolio.name,
          type: 'portfolio' as const,
          sourceId: portfolio.id,
        })),
      });
    }
    return groups;
  }

  private buildInvestmentSelectOptions(holdings: FinanceSecurityHolding[]): InvestmentSelectOption[] {
    const optionById = new Map<string, InvestmentSelectOption>();
    holdings
      .filter(holding => holding.id && holding.positionType !== 'cash')
      .forEach(holding => {
        const existing = optionById.get(holding.id);
        const closed = this.isInvestmentHoldingClosed(holding);
        if (existing) {
          existing.closed = existing.closed && closed;
          return;
        }
        optionById.set(holding.id, {
          id: holding.id,
          name: holding.name,
          symbol: holding.userSymbol || holding.symbol,
          accountName: holding.accountName,
          closed,
        });
      });

    return [...optionById.values()].sort((left, right) => {
      if (left.closed !== right.closed) {
        return left.closed ? 1 : -1;
      }
      return left.name.localeCompare(right.name);
    });
  }

  isInvestmentHoldingClosed(holding: FinanceSecurityHolding | InvestmentSelectOption | null): boolean {
    if (!holding) {
      return false;
    }
    if ('closed' in holding) {
      return holding.closed;
    }
    return Math.abs(holding.quantity ?? 0) < 0.000001;
  }

  loadSummaries(): void {
    this.isLoadingSummaries = true;
    let acId = this.accountId;
    if (acId === '') {
      acId = null;
    }
    this.investmentService.getSummaries(acId, this.includeClosedPositions, '', this.customPortfolioId).subscribe({
      next: (res: InvestmentPortfolioDetails) => {
        this.isLoadingSummaries = false;
        this.summaries = res.summaries;
        this.portfolio = res;
        if (this.selectedPeriod.value != null && this.selectedPeriod.value !== '') {
          this.loadComparisons(acId!);
        }
      },
      error: () => (this.isLoadingSummaries = false),
    });
  }

  findSummary(userSecurityId: string): FinanceInvestmentSnapshotDetails | null {
    if (this.summaries == null) {
      return null;
    }

    let val: FinanceInvestmentSnapshotDetails | null = null;
    for (let i = 0; i < this.summaries.length; i++) {
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (this.summaries[i] != null && this.summaries[i].userSecurityId === userSecurityId) {
        val = this.summaries[i];
        break;
      }
    }
    return val;
  }

  findComparison(userSecurityId: string): FinanceInvestmentSnapshotDetails | null {
    if (this.comparisonPortfolio == null) {
      return null;
    }

    let val: FinanceInvestmentSnapshotDetails | null = null;
    for (let i = 0; i < this.comparisonPortfolio.summaries.length; i++) {
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (this.comparisonPortfolio.summaries[i] != null && this.comparisonPortfolio.summaries[i].userSecurityId === userSecurityId) {
        val = this.comparisonPortfolio.summaries[i];
        break;
      }
    }
    return val;
  }

  total(allHoldings: FinanceSecurityHolding[] | null): number {
    if (allHoldings == null) {
      return 0;
    }

    let value = 0;

    allHoldings.forEach(element => {
      value += this.getHoldingLocalValue(element);
    });

    return value;
  }

  totalSummary(
    allHoldings: FinanceSecurityHolding[] | null,
    capitalInvested: boolean,
    capitalGain: boolean,
    income: boolean,
    currencyGain: boolean,
    totalReturn: boolean,
  ): number {
    if (allHoldings == null) {
      return 0;
    }

    let value = 0;

    allHoldings.forEach(element => {
      if (this.isCashHolding(element)) {
        if (income || totalReturn) {
          value += this.getCashInterestLocalValue(element);
        }
        return;
      }
      // eslint-disable-next-line  @typescript-eslint/no-unnecessary-condition
      let fi = this.findSummary(element.id); //FinanceInvestmentSnapshotDetails
      if (fi) {
        if (capitalInvested) {
          value += fi.totalCapitalInvested;
        } else if (capitalGain) {
          value += fi.totalCapitalGain;
        } else if (income) {
          value += fi.totalIncome;
        } else if (currencyGain) {
          value += fi.totalCurrencyGain;
        } else if (totalReturn) {
          value += fi.totalReturn;
        }
      }
    });

    return value;
  }

  holdingCapitalInvested(holding: FinanceSecurityHolding): number | null {
    return this.isCashHolding(holding) ? null : (this.findSummary(holding.id)?.totalCapitalInvested ?? null);
  }

  holdingCapitalGain(holding: FinanceSecurityHolding): number | null {
    return this.isCashHolding(holding) ? null : (this.findSummary(holding.id)?.totalCapitalGain ?? null);
  }

  holdingIncome(holding: FinanceSecurityHolding): number | null {
    return this.isCashHolding(holding) ? this.getCashInterestLocalValue(holding) : (this.findSummary(holding.id)?.totalIncome ?? null);
  }

  holdingCurrencyGain(holding: FinanceSecurityHolding): number | null {
    return this.isCashHolding(holding) ? null : (this.findSummary(holding.id)?.totalCurrencyGain ?? null);
  }

  holdingTotalReturn(holding: FinanceSecurityHolding): number | null {
    return this.isCashHolding(holding) ? this.getCashInterestLocalValue(holding) : (this.findSummary(holding.id)?.totalReturn ?? null);
  }

  private getCashInterestLocalValue(holding: FinanceSecurityHolding): number {
    const cashInterest = Number(holding.cashInterest ?? 0);
    const fxRate = Number(holding.fxRateToLocal ?? 0);
    const localValue = fxRate !== 0 ? cashInterest * fxRate : cashInterest;
    return Number.isFinite(localValue) ? localValue : 0;
  }

  getCookie(key: string): any {
    return this.cookieService.get(key);
  }

  saveCookie(key: string, value: any): void {
    return this.cookieService.put(key, value);
  }

  private saveScreenState(): void {
    const state: InvestmentPortfolioScreenState = {
      selectedViewId: this.selectedViewId,
      accountId: this.accountId,
      customPortfolioId: this.customPortfolioId,
      includeClosedPositions: this.includeClosedPositions,
      showAnnotations: this.showAnnotations,
      selectedGroupValue: this.selectedGroup.value,
      selectedPeriodValue: this.selectedPeriod.value,
      selectedInvestmentName: this.selectedInvestmentName,
      activeTab: this.activeTab,
      holdingSortColumn: this.holdingSortColumn,
      holdingSortDirection: this.holdingSortDirection,
    };
    this.saveCookie(InvestmentPortfolioComponent.SCREEN_STATE_COOKIE_ID, JSON.stringify(state));
  }

  private restoreScreenState(): void {
    const stateCookie = this.getCookie(InvestmentPortfolioComponent.SCREEN_STATE_COOKIE_ID);
    if (!stateCookie) {
      return;
    }

    let state: InvestmentPortfolioScreenState;
    try {
      state = JSON.parse(stateCookie) as InvestmentPortfolioScreenState;
    } catch {
      return;
    }

    if (typeof state.includeClosedPositions === 'boolean') {
      this.includeClosedPositions = state.includeClosedPositions;
    }
    if (typeof state.showAnnotations === 'boolean') {
      this.showAnnotations = state.showAnnotations;
    }
    if (state.selectedPeriodValue !== undefined) {
      const period = this.periods.find(candidate => candidate.value === state.selectedPeriodValue);
      if (period) {
        this.selectedPeriod = period;
      }
    }
    if (state.selectedGroupValue) {
      const group = this.groupings.find(candidate => candidate.value === state.selectedGroupValue);
      if (group) {
        this.selectedGroup = group;
      }
    }
    if (state.activeTab) {
      this.activeTab = state.activeTab;
    }
    if (state.selectedInvestmentName !== undefined) {
      this.selectedInvestmentName = state.selectedInvestmentName ?? null;
    }
    if (this.isHoldingSortColumn(state.holdingSortColumn)) {
      this.holdingSortColumn = state.holdingSortColumn;
    }
    if (state.holdingSortDirection === 'asc' || state.holdingSortDirection === 'desc') {
      this.holdingSortDirection = state.holdingSortDirection;
    }

    const restoredViewId = state.selectedViewId ?? this.buildViewIdFromState(state);
    if (restoredViewId) {
      this.applySelectedView(restoredViewId);
    }
  }

  private buildViewIdFromState(state: InvestmentPortfolioScreenState): string | null {
    if (state.customPortfolioId) {
      return `portfolio:${state.customPortfolioId}`;
    }
    if (state.accountId !== undefined && state.accountId !== null) {
      return `account:${state.accountId}`;
    }
    return null;
  }

  processPieChartData(): void {
    if (!this.holdings || this.holdings.length === 0) {
      this.setPieChartData([], []);
      return;
    }

    if (this.selectedGroup.value === 'holdings') {
      const pieChartData = this.pieDataByHolding(this.holdings);
      this.setPieChartData(pieChartData.series, pieChartData.labels);
    } else if (this.selectedGroup.groups) {
      const pieChartData = this.pieDataByGroup(this.selectedGroup.groups);
      this.setPieChartData(pieChartData.series, pieChartData.labels);
    } else {
      this.setPieChartData([], []);
    }
  }

  private processTreemapData(): ApexAxisChartSeries {
    if (!this.holdings || this.holdings.length === 0) {
      return this.emptyTreemapSeries;
    }

    if (this.selectedGroup.value === 'holdings') {
      return this.treemapDataByHolding(this.holdings);
    } else if (this.selectedGroup.groups) {
      return this.treemapDataByGroup(this.selectedGroup.groups);
    }

    return this.emptyTreemapSeries;
  }

  private processGroup(group: Group, allHoldings: FinanceSecurityHolding[]): void {
    if (group.value === 'holdings') {
      group.groups = { [group.name]: allHoldings };
      group.groups;
    } else if (group.value === 'portfolio') {
      if (group.groups == null) {
        group.groups = this.groupByPortfolio(allHoldings);
      }
    } else if (group.value === 'currency') {
      if (group.groups == null) {
        group.groups = this.groupByCurrency(allHoldings);
      }
    } else if (group.value === 'sector') {
      if (group.groups == null) {
        group.groups = this.groupBySector(allHoldings);
      }
    } else if (group.value === 'type') {
      if (group.groups == null) {
        group.groups = this.groupByType(allHoldings);
      }
    } else if (group.value === 'industry') {
      if (group.groups == null) {
        group.groups = this.groupByIndustry(allHoldings);
      }
    } else if (group.value === 'exchange') {
      if (group.groups == null) {
        group.groups = this.groupByExchange(allHoldings);
      }
    }

    this.processPieChartData();
    if (this.activeTab === 'treemap') {
      this.setTreemapSeries(this.processTreemapData());
    }

    // ZZZ this.processComparisonData(group.groups!, this.comparisonPortfolio?.summaries);
  }

  private groupByPortfolio(allHoldings: FinanceSecurityHolding[]): GroupData {
    const gdCache: GroupData = {};

    allHoldings.forEach(element => {
      let gd = gdCache[element.accountName];
      if (gd == null) {
        gd = [element];
        gdCache[element.accountName] = gd;
      } else {
        gd.push(element);
      }
    });

    return gdCache;
  }

  private groupByCurrency(allHoldings: FinanceSecurityHolding[]): GroupData {
    const gdCache: GroupData = {};

    allHoldings.forEach(element => {
      let gd = gdCache[element.currencyCode];
      if (gd == null) {
        gd = [element];
        const key = element.currencyCode;

        gdCache[key] = gd;
      } else {
        gd.push(element);
      }
    });

    return gdCache;
  }

  private groupBySector(allHoldings: FinanceSecurityHolding[]): GroupData {
    const gdCache: GroupData = {};

    allHoldings.forEach(element => {
      let key = element.sector;
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (key == null) {
        key = 'Unknown';
      }
      let gd = gdCache[key];
      if (gd == null) {
        gd = [element];

        gdCache[key] = gd;
      } else {
        gd.push(element);
      }
    });

    return gdCache;
  }

  private groupByIndustry(allHoldings: FinanceSecurityHolding[]): GroupData {
    const gdCache: GroupData = {};

    allHoldings.forEach(element => {
      let key = element.industry;
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (key == null) {
        key = 'Unknown';
      }
      let gd = gdCache[key];
      if (gd == null) {
        gd = [element];

        gdCache[key] = gd;
      } else {
        gd.push(element);
      }
    });

    return gdCache;
  }

  private groupByType(allHoldings: FinanceSecurityHolding[]): GroupData {
    const gdCache: GroupData = {};

    allHoldings.forEach(element => {
      let key = element.typeName;
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (key == null) {
        key = 'Unknown';
      }
      let gd = gdCache[key];
      if (gd == null) {
        gd = [element];

        gdCache[key] = gd;
      } else {
        gd.push(element);
      }
    });

    return gdCache;
  }

  private groupByExchange(allHoldings: FinanceSecurityHolding[]): GroupData {
    const gdCache: GroupData = {};

    allHoldings.forEach(element => {
      let key = element.exchangeName;
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (key == null) {
        key = 'Unknown';
      }
      let gd = gdCache[key];
      if (gd == null) {
        gd = [element];

        gdCache[key] = gd;
      } else {
        gd.push(element);
      }
    });

    return gdCache;
  }

  private clearGroupCache() {
    this.groupings.forEach(element => {
      element.groups = null;
    });
  }

  private pieDataByHolding(allHoldings: FinanceSecurityHolding[]): PieChartData {
    const series: number[] = [];
    const labels: string[] = [];

    allHoldings.forEach(element => {
      const value = this.getHoldingLocalValue(element);

      if (!Number.isFinite(value)) {
        return;
      }

      series.push(value);
      labels.push(element.name || 'Unknown');
    });

    return { series, labels };
  }

  private treemapDataByHolding(allHoldings: FinanceSecurityHolding[]): ApexAxisChartSeries {
    const series = this.dataByHolding(allHoldings, 'xy');
    return series!;
  }

  private dataByHolding(allHoldings: FinanceSecurityHolding[], type: string): ApexAxisChartSeries | null {
    let xydataArr = [
      {
        x: '',
        y: 0,
      },
    ];
    xydataArr = [];

    allHoldings.forEach(element => {
      const xy = { x: '', y: 0 };
      const value = this.getHoldingLocalValue(element);

      if (type === 'xy') {
        xy.x = element.name || 'Unknown';
        xy.y = value;
        xydataArr.push(xy);
      }
    });

    if (type === 'xy') {
      const xyseries = [{ data: xydataArr }];
      xydataArr.sort((a, b) => (a.y > b.y ? -1 : a.y === b.y ? (a.y > b.y ? 1 : -1) : 1));
      return xyseries;
    }

    return null;
  }

  private pieDataByGroup(groupData: GroupData): PieChartData {
    const series: number[] = [];
    const labels: string[] = [];
    const keys = Object.keys(groupData);
    keys.forEach(key => {
      const holding = groupData[key];
      if (holding !== null) {
        const val = this.sumHoldings(holding);
        series.push(val);
        labels.push(key);
      }
    });

    return { series, labels };
  }

  private treemapDataByGroup(groupData: GroupData): ApexAxisChartSeries {
    let xydataArr = [
      {
        x: '',
        y: 0,
      },
    ];
    xydataArr = [];
    const keys = Object.keys(groupData);
    keys.forEach(key => {
      const holding = groupData[key];
      if (holding !== null) {
        const val = this.sumHoldings(holding);
        const xy = { x: '', y: 0 };
        xy.y = val;
        xy.x = key;
        xydataArr.push(xy);
      }
    });

    xydataArr.sort((a, b) => (a.y > b.y ? -1 : a.y === b.y ? (a.y > b.y ? 1 : -1) : 1));
    const xyseries = [{ name: this.selectedGroup.name, data: xydataArr }];
    return xyseries;
  }

  private sumHoldings(holdings: FinanceSecurityHolding[]): number {
    let val = 0;
    holdings.forEach(holding => {
      val += this.getHoldingLocalValue(holding);
    });
    return val;
  }

  private getHoldingLocalValue(holding: FinanceSecurityHolding): number {
    const baseValue = Number(holding.value ?? 0);
    const fxRate = Number(holding.fxRateToLocal ?? 0);
    const localValue = fxRate !== 0 ? baseValue * fxRate : baseValue;

    return Number.isFinite(localValue) ? localValue : 0;
  }

  // History / Over-time
  loadHistory(): void {
    // eslint-disable-next-line no-console
    console.log('Loading History');
    this.isLoadingHistory = true;

    let acId = this.accountId;
    if (acId === '') {
      acId = null;
    }
    this.investmentService
      .getPortfolioHistory(acId, this.includeClosedPositions, this.getPeriodString(), this.customPortfolioId)
      .subscribe({
        next: (res: FinanceResourceSnapshots[]) => {
          // eslint-disable-next-line no-console
          console.log('Done Loading History');
          this.isHistoryLoaded = true;
          this.isLoadingHistory = false;
          this.portfolioValueHistoryItems = res;
          //this.historyChartOptions.series = this.configureHistorySeriesPerHolding(this.portfolioValueHistoryItems!, 'xy');

          // this.selectedGroup.groups
          if (this.selectedGroup.groups != null) {
            this.setHistorySeries(this.configureHistorySeriesByGroup(this.selectedGroup.groups, this.portfolioValueHistoryItems));
          } else {
            this.setHistorySeries(this.configureHistorySeriesNoGrouping(this.portfolioValueHistoryItems));
          }

          if (this.showAnnotations) {
            this.setHistoryAnnotations(this.processAnnotations(this.portfolioValueHistoryItems));
          } else {
            this.setHistoryAnnotations({});
          }
          this.refreshHistoryChart();
        },
        error: () => ((this.isLoadingHistory = false), (this.isHistoryLoaded = true), (this.showHistoryChart = false)),
      });
  }

  public isNaN(num: unknown): boolean {
    //console.log('Checking if NaN: ' + num + ' isNaN: ' + Number.isNaN(num));
    if (num === null || num === undefined) {
      return true;
    }

    if (num === '' || num === 'null' || num === 'undefined' || num === 'NaN') {
      return true;
    }
    return Number.isNaN(num);
  }

  private configureHistorySeriesByGroup(groupData: GroupData, portfolioValueHistoryItems: FinanceResourceSnapshots[]): ApexAxisChartSeries {
    let xydataArr = [{ x: 0, y: 0 }];
    let serieses: ApexAxisChartSeries = []; // {data:[0]},{data:[0]}

    if (portfolioValueHistoryItems == null) {
      return this.emptyHistorySeries;
    }
    // eslint-disable-next-line no-console
    console.log('Processing History with By Group');

    //let map: Map<String, any> = new Map<String, any>();
    let securityToGroupMap = this.mapSecurityIntoGroups(groupData);
    let map: Map<string, any> = new Map<string, any>();
    let groupSeriesMap: Map<string, any> = new Map<string, any>();

    for (let i = 0; i < portfolioValueHistoryItems.length; i++) {
      let portfolioValueHistoryItem: FinanceResourceSnapshots = portfolioValueHistoryItems[i];
      // Work out what group this security is in....
      let groupName = securityToGroupMap.get(portfolioValueHistoryItem.id);
      if (groupName == null) {
        // eslint-disable-next-line no-console
        console.log('Cannot find group for: ' + portfolioValueHistoryItem.name);
        groupName = '[Closed/Historic]';
      }
      // Get the right data series now (create the series if not created)
      let xyseries = groupSeriesMap.get(groupName!);

      if (xyseries === undefined) {
        xydataArr = [];
        xyseries = { name: groupName!, data: xydataArr };
        serieses.push(xyseries);
        groupSeriesMap.set(groupName!, xyseries);
      } else {
        xydataArr = xyseries.data;
      }

      portfolioValueHistoryItem.snapshots?.forEach((element: { date: string; value: number }) => {
        // eslint-disable-next-line no-console
        console.log('Processing security values for: ' + portfolioValueHistoryItem.name);

        let time = Date.parse(element.date);
        if (Number.isNaN(time) || element.value == null) {
          return;
        }

        let xy = map.get(groupName + time);
        if (xy == null) {
          xy = { x: time, y: 0, time };
          map.set(groupName + time, xy);
          console.log('Created new Date holder in series for: ' + element.date);
          xydataArr.push(xy);
        }
        if (element.date === '2023-02-02') {
          // eslint-disable-next-line no-console
          console.log(
            'Date: ' +
              element.date +
              ' - $' +
              element.value +
              ', being added to existing value: $' +
              xy.y +
              '. Now is: ' +
              (xy.y + element.value),
          );
        }
        xy.y = xy.y + element.value;
      });
    }

    // Sort everything
    serieses = serieses
      .filter((element: any) => Array.isArray(element?.data))
      .map((element: any) => {
        let sortedData = element.data.sort(function (a: any, b: any) {
          return a.time - b.time;
        });
        element.data = sortedData;
        return element;
      })
      .filter((element: any) => element.data.length > 0);

    if (serieses.length === 0) {
      return this.emptyHistorySeries;
    }

    return serieses;
  }

  private mapSecurityIntoGroups(groupData: GroupData): Map<string, string> {
    let map: Map<string, string> = new Map<string, string>();

    Object.keys(groupData).forEach((groupValue: string) => {
      groupData[groupValue]?.forEach((value: FinanceSecurityHolding) => {
        map.set(value.id, groupValue);
      });
    });

    return map;
  }

  private configureHistorySeriesPerHolding(portfolioValueHistoryItems: PortfolioValueHistoryItem[], type: string): ApexAxisChartSeries {
    let xydataArr = [{ x: 0, y: 0 }];

    let serieses: ApexAxisChartSeries = []; // {data:[0]},{data:[0]}

    for (let i = 0; i < portfolioValueHistoryItems.length; i++) {
      let portfolioValueHistoryItem: PortfolioValueHistoryItem = portfolioValueHistoryItems[i];
      xydataArr = [];
      const xyseries = { name: portfolioValueHistoryItem.security.name, data: xydataArr };
      serieses.push(xyseries);

      portfolioValueHistoryItem.valueOverTimePerSecurity.forEach((element: { date: string; value: number }) => {
        let d = Date.parse(element.date);
        if (Number.isNaN(d)) {
          return;
        }
        const xy = { x: d, y: Math.round(element.value), time: d };
        xydataArr.push(xy);
      });
    }

    return serieses;
  }

  private configureHistorySeriesNoGrouping(portfolioValueHistoryItems: FinanceResourceSnapshots[]): ApexAxisChartSeries {
    let xydataArr = [{ x: 0, y: 0, time: 0 }];

    xydataArr = [];
    // eslint-disable-next-line no-console
    console.log('Processing History with no grouping');

    let map: Map<string, any> = new Map<string, any>();
    for (let i = 0; i < portfolioValueHistoryItems.length; i++) {
      let portfolioValueHistoryItem: FinanceResourceSnapshots = portfolioValueHistoryItems[i];
      // eslint-disable-next-line no-console
      console.log('Processing security values for: ' + portfolioValueHistoryItem.name);

      portfolioValueHistoryItem.snapshots?.forEach((element: { date: string; value: number }) => {
        let time = Date.parse(element.date);
        if (Number.isNaN(time)) {
          return;
        }
        let xy = map.get(String(time));
        if (xy == null) {
          xy = { x: time, y: 0, time };
          map.set(String(time), xy);
          xydataArr.push(xy);
        }
        xy.y = xy.y + Math.round(element.value);
      });
    }

    let elements = xydataArr.sort(function (a, b) {
      return a.time - b.time;
    });

    if (elements.length === 0) {
      return this.emptyHistorySeries;
    }

    let serieses: ApexAxisChartSeries = []; // {data:[0]},{data:[0]}
    const xyseries = { name: 'All Holdings', data: elements };
    serieses.push(xyseries);

    return serieses;
  }

  private processAnnotations(portfolioValueHistoryItems: FinanceResourceSnapshots[]): ApexAnnotations {
    let xAnnotations: XAxisAnnotations[] = [];
    let annotations: ApexAnnotations = { xaxis: xAnnotations };

    for (let i = 0; i < portfolioValueHistoryItems.length; i++) {
      let portfolioValueHistoryItem: FinanceResourceSnapshots = portfolioValueHistoryItems[i];

      portfolioValueHistoryItem.annotations.forEach(
        (element: { date: string; annotation: string; type: string; quantity: number; price: number; totalValue: number }) => {
          let d = Date.parse(element.date);
          let bg = '#fff';
          if (this.theme == 'dark') {
            bg = 'var(--app-colours-background-high-dark)';
          }
          let xAnnotation: XAxisAnnotations = { borderColor: 'var(--app-colours-primary)', fillColor: bg };
          //xAnnotation.x = this.pipe.transform(d, 'MM/dd/yyyy')!;
          xAnnotation.x = d;
          let symbol = portfolioValueHistoryItem.symbol;
          if (!symbol || symbol == null) {
            symbol = portfolioValueHistoryItem.name;
          }

          xAnnotation.label = { text: symbol + ' ' + element.annotation, style: { color: 'var(--app-colours-primary)', background: bg } };
          xAnnotations.push(xAnnotation);
          // xy.y = Math.round(element.value);
          // xydataArr.push(xy);
        },
      );
    }

    return annotations;
  }

  private getPeriodString(): string {
    if (this.selectedPeriod.value == 'ALL') {
      return '';
    } else return this.selectedPeriod.value;
  }

  private setHistorySeries(series: ApexAxisChartSeries): void {
    this.historyChartOptions = {
      ...this.historyChartOptions,
      series: this.normalizeTimeSeries(series, 'All Holdings'),
    };
  }

  private setHistoryAnnotations(annotations: ApexAnnotations): void {
    this.historyChartOptions = {
      ...this.historyChartOptions,
      annotations,
    };
  }

  private setTreemapSeries(series: ApexAxisChartSeries): void {
    this.treemapChartOptions = {
      ...this.treemapChartOptions,
      series: this.normalizeTreemapSeries(series, this.selectedGroup.name),
    };
  }

  private setPieChartData(series: number[], labels: string[]): void {
    this.pieChartOptions = {
      ...this.pieChartOptions,
      series: [...series],
      labels: [...labels],
    };
    this.refreshPieChart();
  }

  private normalizeTimeSeries(series: ApexAxisChartSeries | null | undefined, defaultName: string): ApexAxisChartSeries {
    if (!Array.isArray(series) || series.length === 0) {
      return [{ name: defaultName, data: [] }];
    }

    const normalizedSeries = series
      .filter((entry: any) => entry != null)
      .map((entry: any, index: number) => ({
        name: entry.name ?? `${defaultName} ${index + 1}`,
        data: Array.isArray(entry.data)
          ? entry.data
              .filter((point: any) => point != null)
              .map((point: any) => ({
                x: typeof point.x === 'number' ? point.x : Date.parse(point.x),
                y: typeof point.y === 'number' ? point.y : Number(point.y ?? 0),
              }))
              .filter((point: { x: number; y: number }) => !Number.isNaN(point.x) && !Number.isNaN(point.y))
          : [],
      }))
      .filter((entry: any) => Array.isArray(entry.data));

    if (normalizedSeries.length === 0) {
      return [{ name: defaultName, data: [] }];
    }

    return normalizedSeries;
  }

  private normalizeTreemapSeries(series: ApexAxisChartSeries | null | undefined, defaultName: string): ApexAxisChartSeries {
    if (!Array.isArray(series) || series.length === 0) {
      return [{ name: defaultName, data: [] }];
    }

    const normalizedSeries = series
      .filter((entry: any) => entry != null)
      .map((entry: any, index: number) => ({
        name: entry.name ?? `${defaultName} ${index + 1}`,
        data: Array.isArray(entry.data)
          ? entry.data
              .filter((point: any) => point != null)
              .map((point: any) => ({
                x: String(point.x ?? ''),
                y: typeof point.y === 'number' ? point.y : Number(point.y ?? 0),
              }))
              .filter((point: { x: string; y: number }) => point.x.length > 0 && !Number.isNaN(point.y))
          : [],
      }))
      .filter((entry: any) => Array.isArray(entry.data));

    if (normalizedSeries.length === 0) {
      return [{ name: defaultName, data: [] }];
    }

    return normalizedSeries;
  }

  private refreshHistoryChart(): void {
    this.showHistoryChart = false;
    this.cdr.detectChanges();
    this.showHistoryChart = true;
    this.cdr.detectChanges();
    setTimeout(() => this.applyThemeToCharts(), 0);
  }

  private refreshPieChart(): void {
    this.showPieChart = false;
    this.cdr.detectChanges();
    this.showPieChart = true;
    this.cdr.detectChanges();
    setTimeout(() => this.syncPieChart(), 0);
  }

  private syncPieChart(): void {
    if (!this.pieChart) {
      return;
    }

    const series = Array.isArray(this.pieChartOptions.series) ? [...this.pieChartOptions.series] : [];
    const labels = Array.isArray(this.pieChartOptions.labels) ? [...this.pieChartOptions.labels] : [];

    void this.pieChart.updateOptions({ labels }, false, true, true);
    void this.pieChart.updateSeries(series, true);
  }

  private applyThemeToCharts(): void {
    const chartTheme = this.getChartThemeOptions();

    this.pieChartOptions = {
      ...this.pieChartOptions,
      chart: this.withChartSurface(this.pieChartOptions.chart, { ...chartTheme, background: 'transparent' }),
      theme: {
        ...(this.pieChartOptions.theme ?? {}),
        mode: this.theme as 'light' | 'dark',
      },
      tooltip: {
        ...this.pieChartOptions.tooltip,
        theme: this.theme,
      },
    };

    this.treemapChartOptions = {
      ...this.treemapChartOptions,
      chart: this.withChartSurface(this.treemapChartOptions.chart, chartTheme),
      tooltip: {
        ...this.treemapChartOptions.tooltip,
        theme: this.theme,
      },
      theme: {
        mode: this.theme as 'light' | 'dark',
      },
    };

    this.historyChartOptions = {
      ...this.historyChartOptions,
      chart: this.withChartSurface(this.historyChartOptions.chart, chartTheme),
      options: {
        ...this.historyChartOptions.options,
        grid: {
          borderColor: chartTheme.gridBorderColor,
        },
        theme: {
          mode: this.theme as 'light' | 'dark',
        },
        tooltip: {
          ...this.historyChartOptions.options?.tooltip,
          theme: this.theme,
        },
      },
      xaxis: {
        ...this.historyChartOptions.xaxis,
        labels: {
          ...this.historyChartOptions.xaxis?.labels,
          style: {
            ...(this.historyChartOptions.xaxis?.labels as any)?.style,
            colors: chartTheme.axisLabelColor,
          },
        },
      },
      yaxis: {
        ...this.historyChartOptions.yaxis,
        labels: {
          ...this.historyChartOptions.yaxis?.labels,
          style: {
            ...(this.historyChartOptions.yaxis?.labels as any)?.style,
            colors: chartTheme.axisLabelColor,
          },
        },
      },
    };

    if (this.pieChart) {
      void this.pieChart.updateOptions(
        {
          chart: this.pieChartOptions.chart,
          theme: this.pieChartOptions.theme,
          tooltip: this.pieChartOptions.tooltip,
        },
        false,
        true,
        true,
      );
    }

    if (this.treemapChart) {
      void this.treemapChart.updateOptions(
        {
          chart: this.treemapChartOptions.chart,
          theme: this.treemapChartOptions.theme,
          tooltip: this.treemapChartOptions.tooltip,
        },
        false,
        true,
        true,
      );
    }

    if (this.historyChart) {
      void this.historyChart.updateOptions(
        {
          chart: this.historyChartOptions.chart,
          grid: this.historyChartOptions.options?.grid,
          theme: this.historyChartOptions.options?.theme,
          tooltip: this.historyChartOptions.options?.tooltip,
          xaxis: this.historyChartOptions.xaxis,
          yaxis: this.historyChartOptions.yaxis,
        },
        false,
        true,
        true,
      );
    }
  }

  private getChartThemeOptions(): {
    axisLabelColor: string[];
    background: string;
    foreColor: string;
    gridBorderColor: string;
  } {
    if (this.theme === 'dark') {
      return {
        axisLabelColor: ['var(--app-colours-secondary-text-dark)'],
        background: 'var(--app-colours-background-high-dark)',
        foreColor: 'var(--app-colours-primary-text-dark)',
        gridBorderColor: 'var(--app-colours-border-low-dark)',
      };
    }

    return {
      axisLabelColor: ['var(--app-colours-secondary-text-light)'],
      background: 'var(--app-colours-background-high-light)',
      foreColor: 'var(--app-colours-primary-text-light)',
      gridBorderColor: 'var(--app-colours-border-low-light)',
    };
  }

  private withChartSurface(chart: ApexChart | undefined, chartTheme: { background: string; foreColor: string }): ApexChart | undefined {
    if (!chart) {
      return chart;
    }

    return {
      ...chart,
      background: chartTheme.background,
      foreColor: chartTheme.foreColor,
    };
  }
}
