import { ChangeDetectorRef, Component, Inject, LOCALE_ID, NgZone, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ThemeChangeEvent, ThemeService } from 'app/layouts/main/theme.service';
import { InvestmentPortfolio } from './investment-portfolio.service';
import {
  FinanceInvestmentSnapshotDetails,
  FinanceResourceSnapshots,
  FinanceSecurityHolding,
  FinancialAccount,
  InvestmentPortfolioDetails,
  Periods,
  PortfolioValueHistoryItem,
} from '../finance.model';
import { formatCurrency, formatDate } from '@angular/common';
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
import { DropdownModule } from 'primeng/dropdown';
import { InputSwitchModule } from 'primeng/inputswitch';
import { SkeletonModule } from 'primeng/skeleton';
import { DeltaValueComponent } from 'app/shared/delta-value/delta-value.component';

interface GroupData {
  [name: string]: FinanceSecurityHolding[] | null;
}

interface Group {
  name: string;
  value: string;
  groups: GroupData | null;
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
  dataLabels: ApexDataLabels;
  tooltip: ApexTooltip;
};

@Component({
  selector: 'jhi-investment-portfolio',
  templateUrl: './investment-portfolio.component.html',
  styleUrls: ['./investment-portfolio.component.scss'],
  imports: [SharedModule, RouterModule, DropdownModule, NgApexchartsModule, InputSwitchModule, SkeletonModule],
})
export class InvestmentPortfolioComponent {
  // implements OnInit
  static COOKIE_SHOW_CLOSED = 'show-closed-state';
  static PERIOD_DEFAULT_COOKIE_ID = 'period-default';

  static ALL_ACCOUNTS: FinancialAccount = {
    id: '',
    name: 'All',
    type: -1,
    accountType: 'All',
    currencyCode: '',
    balance: 0,
    balanceWarning: '',
    fxDateTime: null,
    relatedToAccountId: null,
    institution: null,
    startingBalance: 0,
    fxRateToLocal: null,
  };

  title = '';
  theme = 'light';
  isLoading = false;
  isLoadingSummaries = false;
  isLoadingHistory = false;
  isHistoryLoaded = false;

  accountId: string | null = null;
  account: FinancialAccount | null = null;
  groupings: Group[];

  includeClosedPositions = false;

  showAnnotations = false;

  selectedGroup: Group;

  periods: Periods[];
  selectedPeriod: Periods;

  selectedInvestmentName: string | null = null;

  /* Tabs */
  mainTab: string;
  activeTab: string;

  // pipe = new DatePipe('en-US');

  steadyVariance = 0.01; // mark as steady if not more or less than this

  // Data that gets displayed in the grid
  public holdings!: FinanceSecurityHolding[] | null;

  public accounts!: FinancialAccount[] | null;

  public summaries!: FinanceInvestmentSnapshotDetails[] | null;

  public portfolio!: InvestmentPortfolioDetails | null;

  public portfolioValueHistoryItems!: FinanceResourceSnapshots[] | null;

  public comparisonPortfolio!: InvestmentPortfolioDetails | null; // The period to compare against

  /* Charts */
  @ViewChild('pieChart', { static: false }) pieChart!: ChartComponent;
  public pieChartOptions: Partial<ChartOptions>;

  @ViewChild('treemapChart', { static: false }) treemapChart!: ChartComponent;
  public treemapChartOptions: Partial<TreeMapChartOptions>;

  @ViewChild('historyChart', { static: false }) historyChart!: ChartComponent;
  public historyChartOptions: Partial<AreaChartOptions>;

  constructor(
    private investmentService: InvestmentPortfolio,
    private route: ActivatedRoute,
    private router: Router,
    private themeService: ThemeService,
    private accountService: AccountList,
    private cookieService: CookieService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef,
    @Inject(LOCALE_ID) private locale: string,
  ) {
    //, private commonControllerServices:CommonControllerServices
    this.groupings = [
      { name: 'Holdings', value: 'holdings', groups: null },
      { name: 'Portfolio', value: 'portfolio', groups: null },
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
      series: [],
      legend: {
        show: true,
      },
      title: {},
      plotOptions: {
        treemap: {
          enableShades: true,
          distributed: true,
        },
      },
    };

    this.historyChartOptions = {};

    this.mainTab = 'holdings';
    this.activeTab = 'holdings';
  }

  ngOnInit(): void {
    this.theme = this.themeService.theme();

    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;

      this.historyChart.updateOptions({ tooltip: { theme: this.theme } });
    });

    //this.title = this.commonControllerServices.getPageTitle(this.router.routerState.snapshot.root);

    this.route.params.subscribe(params => {
      if (params['id']) {
        this.accountId = params['id'];
      }
    });

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

    (this.treemapChartOptions.chart = {
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
        // eslint-disable-next-line object-shorthand
        formatter:
          // function (val: string, op: any): string[] {
          //   return [val, ptr.currencyFormatter(op.value)]; // + '<br>' + String(op.value)
          // },
          function (val: string, op: any): string | number {
            return ptr.currencyFormatter(op.value); // + '<br>' + String(op.value)
          },
      });

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
      y: {
        // eslint-disable-next-line object-shorthand
        formatter: function (val: number, ops: any): string {
          return ptr.currencyFormatter(val); //, ptr.currencyFormatter(ops.value)]; // + '<br>' + String(op.value)
        },
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

    this.load();

    //this.historyChart.updateOptions({chart:{theme:this.theme}});
    //this.historyChart.updateOptions({theme:this.theme});
  }

  navigateToInvestmentFromSeriesIndex(series: number, index: number): void {
    const dp = this.treemapChartOptions.series![series].data[index] as { x: any; y: any };

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
      this.router.navigate(['/', 'finance', 'investment', id], { skipLocationChange: false }).then(
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
    if (this.accountId || this.accountId === '') {
      this.account = this.getAccount(this.accountId);
      this.load();
      // Clear Groups
      this.clearGroupCache();
    }
  }

  onChangeGrouping(event: any): void {
    if (this.holdings) {
      this.selectedInvestmentName = null;
      this.processGroup(this.selectedGroup, this.holdings);
      if (this.isHistoryLoaded) {
        let series = this.configureHistorySeriesByGroup(this.selectedGroup.groups!, this.portfolioValueHistoryItems!);
        this.historyChart.updateSeries(series);
      }
    }
  }

  onChangePeriod(event: any): void {
    this.zone.run(() => {
      this.comparisonPortfolio = null;
      this.saveCookie(InvestmentPortfolioComponent.PERIOD_DEFAULT_COOKIE_ID, this.selectedPeriod.value);
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

  loadComparisons(acId: string) {
    this.investmentService.getSummaries(acId, this.includeClosedPositions, this.selectedPeriod.value).subscribe({
      next: (res: InvestmentPortfolioDetails) => {
        this.comparisonPortfolio = res;
      },
      error: () => (this.isLoadingSummaries = false),
    });
  }

  clickTab(tabName: string): void {
    // // console.log('activetab pre: ' + this.activetab);
    this.activeTab = tabName;
    if (this.activeTab === 'treemap') {
      this.treemapChartOptions.series = this.processTreemapData();
    } else if (this.activeTab === 'history') {
      //this.historyChartOptions.series = this.processTreemapData();
      //this.loadHistory();
    }
  }

  onPieClick(event: any, selectedDataPoint: number): void {
    // eslint-disable-next-line no-console
    console.log('event: ' + JSON.stringify(selectedDataPoint));

    this.zone.run(() => (this.selectedInvestmentName = this.pieChartOptions.labels![selectedDataPoint]));
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
    this.investmentService.get(acId, this.includeClosedPositions).subscribe({
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

  onChangePositionsSwitch(event: any): void {
    this.saveCookie(InvestmentPortfolioComponent.COOKIE_SHOW_CLOSED, this.includeClosedPositions);
    this.load();
  }

  onChangeAnnotationsSwitch(event: any): void {
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

  loadSummaries(): void {
    this.isLoadingSummaries = true;
    let acId = this.accountId;
    if (acId === '') {
      acId = null;
    }
    this.investmentService.getSummaries(acId, this.includeClosedPositions, '').subscribe({
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
      // eslint-disable-next-line  @typescript-eslint/no-unnecessary-condition
      if (element.fxRateToLocal != null && element.fxRateToLocal !== 0) {
        value += element.value * element.fxRateToLocal;
      } else {
        value += element.value;
      }
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

  getCookie(key: string): any {
    return this.cookieService.get(key);
  }

  saveCookie(key: string, value: any): void {
    return this.cookieService.put(key, value);
  }

  processPieChartData(): void {
    // console.log("processingPieChartData: " + this.selectedGroup.value);

    this.pieChartOptions.series = [];
    if (this.pieChartOptions.labels) {
      this.pieChartOptions.labels.length = 0;
    }

    if (this.selectedGroup.value === 'holdings') {
      this.pieDataByHolding(this.holdings!);
    } else {
      this.pieDataByGroup(this.selectedGroup.groups!);
    }
  }

  private processTreemapData(): ApexAxisChartSeries {
    if (this.selectedGroup.value === 'holdings') {
      return this.treemapDataByHolding(this.holdings!);
    } else {
      return this.treemapDataByGroup(this.selectedGroup.groups!);
    }
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
      this.treemapChartOptions.series = this.processTreemapData();
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

  private pieDataByHolding(allHoldings: FinanceSecurityHolding[]): ApexAxisChartSeries | null {
    return this.dataByHolding(allHoldings, 'pie');
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

      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (element.fxRateToLocal === null || element.fxRateToLocal === 0) {
        if (type === 'pie') {
          this.pieChartOptions.series?.push(element.value);
        } else if (type === 'xy') {
          xy.y = element.value;
        }
      } else {
        if (type === 'pie') {
          this.pieChartOptions.series?.push(element.value * element.fxRateToLocal);
          xy.y = element.value * element.fxRateToLocal;
        }
        if (type === 'xy') {
          xy.y = element.value * element.fxRateToLocal;
        }
      }

      if (type === 'pie') {
        this.pieChartOptions.labels?.push(element.name);
      } else {
        xy.x = element.name;
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

  private pieDataByGroup(groupData: GroupData): void {
    const keys = Object.keys(groupData);
    keys.forEach(key => {
      const holding = groupData[key];
      if (holding !== null) {
        const val = this.sumHoldings(holding);

        this.pieChartOptions.series?.push(val);
        this.pieChartOptions.labels?.push(key);
      }
    });
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
      // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
      if (holding.fxRateToLocal === null || holding.fxRateToLocal === 0) {
        val = val + holding.value;
      } else {
        val = val + holding.value * holding.fxRateToLocal;
      }
    });
    return val;
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
    this.investmentService.getPortfolioHistory(acId, this.includeClosedPositions, this.getPeriodString()).subscribe({
      next: (res: FinanceResourceSnapshots[]) => {
        // eslint-disable-next-line no-console
        console.log('Done Loading History');
        this.isHistoryLoaded = true;
        this.isLoadingHistory = false;
        this.portfolioValueHistoryItems = res;
        //this.historyChartOptions.series = this.configureHistorySeriesPerHolding(this.portfolioValueHistoryItems!, 'xy');

        // this.selectedGroup.groups
        if (this.selectedGroup.groups != null) {
          this.historyChartOptions.series = this.configureHistorySeriesByGroup(
            this.selectedGroup.groups!,
            this.portfolioValueHistoryItems!,
          );
        } else {
          this.historyChartOptions.series = this.configureHistorySeriesNoGrouping(this.portfolioValueHistoryItems!);
        }
        if (this.showAnnotations) {
          this.historyChartOptions.annotations = this.processAnnotations(this.portfolioValueHistoryItems!);
        } else {
          this.historyChartOptions.annotations = {};
        }
      },
      error: () => ((this.isLoadingHistory = false), (this.isHistoryLoaded = true)),
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
    let xydataArr = [{ x: '', y: 0 }];

    let serieses: ApexAxisChartSeries = []; // {data:[0]},{data:[0]}

    if (portfolioValueHistoryItems == null) {
      return serieses;
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

      portfolioValueHistoryItem.snapshots.forEach((element: { date: string; value: number }) => {
        // eslint-disable-next-line no-console
        console.log('Processing security values for: ' + portfolioValueHistoryItem.name);

        let time = Date.parse(element.date);
        const format = 'MM/dd/yyyy';
        let x = formatDate(time, format, this.locale);
        let xy = map.get(groupName + x);
        if (xy == null) {
          xy = { x: '', y: 0, d: null };
          xy.time = time;
          xy.x = x;
          map.set(groupName + x, xy);
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
    serieses.forEach((element: any) => {
      let sortedData = element.data.sort(function (a: any, b: any) {
        return a.time - b.time;
      });
      element.data = sortedData;
    });

    return serieses;
  }

  private mapSecurityIntoGroups(groupData: GroupData): Map<string, string> {
    // Object.values(groupData).forEach((value: FinanceSecurityHolding[] | null, index: number) => {
    // });
    let map: Map<string, string> = new Map<string, string>();

    Object.keys(groupData).forEach((groupValue: string, groupIndex: number) => {
      groupData[groupValue]!.forEach((value: FinanceSecurityHolding, index: number) => {
        map.set(value.id, groupValue);
      });
    });

    return map;
  }

  private configureHistorySeriesPerHolding(portfolioValueHistoryItems: PortfolioValueHistoryItem[], type: string): ApexAxisChartSeries {
    let xydataArr = [{ x: '', y: 0 }];

    let serieses: ApexAxisChartSeries = []; // {data:[0]},{data:[0]}

    for (let i = 0; i < portfolioValueHistoryItems.length; i++) {
      let portfolioValueHistoryItem: PortfolioValueHistoryItem = portfolioValueHistoryItems[i];
      xydataArr = [];
      const xyseries = { name: portfolioValueHistoryItem.security.name, data: xydataArr };
      serieses.push(xyseries);

      portfolioValueHistoryItem.valueOverTimePerSecurity.forEach((element: { date: string; value: number }) => {
        const xy = { x: '', y: 0 };
        let d = Date.parse(element.date);
        //xy.x = this.datePipe.transform(d, 'MM/dd/yyyy')!;
        const format = 'MM/dd/yyyy';
        xy.x = formatDate(d, format, this.locale);
        xy.y = Math.round(element.value);
        xydataArr.push(xy);
      });
    }

    return serieses;
  }

  private configureHistorySeriesNoGrouping(portfolioValueHistoryItems: FinanceResourceSnapshots[]): ApexAxisChartSeries {
    let xydataArr = [{ x: '', y: 0, time: 0 }];

    xydataArr = [];
    // eslint-disable-next-line no-console
    console.log('Processing History with no grouping');

    let map: Map<String, any> = new Map<String, any>();
    for (let i = 0; i < portfolioValueHistoryItems.length; i++) {
      let portfolioValueHistoryItem: FinanceResourceSnapshots = portfolioValueHistoryItems[i];
      // eslint-disable-next-line no-console
      console.log('Processing security values for: ' + portfolioValueHistoryItem.name);

      portfolioValueHistoryItem.snapshots.forEach((element: { date: string; value: number }) => {
        let time = Date.parse(element.date);
        //let x = this.datePipe.transform(time, 'MM/dd/yyyy')!;
        const format = 'MM/dd/yyyy';
        let x = formatDate(time, format, this.locale);
        let xy = map.get(x);
        if (xy == null) {
          // eslint-disable-next-line no-console
          console.log('Date: ' + xy.time + ' not found in array. Adding new holder.');
          xy = { x: '', y: 0, d: null };
          xy.time = time;
          xy.x = x;
          map.set(x, xy);
        }
        // eslint-disable-next-line no-console
        console.log(
          'Date: ' +
            xy.time +
            ' - ' +
            Math.round(element.value) +
            ', being added to existing value: ' +
            xy.y +
            '. Now is: ' +
            (xy.y + Math.round(element.value)),
        );

        xy.y = xy.y + Math.round(element.value);
        xydataArr.push(xy);
      });
    }

    let elements = xydataArr.sort(function (a, b) {
      return a.time - b.time;
    });

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
}
