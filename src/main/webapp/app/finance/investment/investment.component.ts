import { Component, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ThemeChangeEvent, ThemeService } from 'app/layouts/main/theme.service';
import { InvestmentTransactions } from './investment.service';
import {
  FinanceInvestmentSnapshotDetails,
  FinanceResourceSnapshots,
  FinanceSecurityHolding,
  InvestmentTransaction,
} from '../finance.model';
import { DatePipe, CurrencyPipe, formatCurrency } from '@angular/common';
import { AccountList } from '../account-list/account-list.service';
// import { CommonControllerServices } from 'app/core/util/common-controller.service';
import { InvestmentPortfolio } from '../investment-portfolio/investment-portfolio.service';
import { InvestmentPortfolioComponent } from '../investment-portfolio/investment-portfolio.component';
import { CookieService } from 'ngx-cookie';
import {
  ApexAnnotations,
  ApexAxisChartSeries,
  ApexChart,
  ApexOptions,
  ApexTitleSubtitle,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  ChartComponent,
  NgApexchartsModule,
  XAxisAnnotations,
} from 'ng-apexcharts';
import SharedModule from 'app/shared/shared.module';

interface GroupData {
  [name: string]: FinanceSecurityHolding[] | null;
}

interface Group {
  name: string;
  value: string;
  groups: GroupData | null;
}

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

@Component({
  selector: 'jhi-investment',
  templateUrl: './investment.component.html',
  styleUrls: ['./investment.component.scss'],
  imports: [SharedModule, NgApexchartsModule],
})
export class InvestmentComponent {
  // implements OnInit
  title = '';
  theme = 'light';
  isLoading = false;
  isSummaryLoading = false;
  isLoadingHistory = false;

  securityId: string | null = null;
  holding: FinanceSecurityHolding | null = null;
  includeClosedPositions = false;

  public holdings!: FinanceSecurityHolding[] | null;

  //public investmen2!: InvestmentDetails | null;
  public investmentSummary!: FinanceInvestmentSnapshotDetails | null;
  public marketValue = 0;
  public transactions!: InvestmentTransaction[] | null;

  @ViewChild('historyChart', { static: false }) historyChart!: ChartComponent;
  public historyChartOptions: Partial<AreaChartOptions>;
  public portfolioValueHistoryItems!: FinanceResourceSnapshots[] | null;
  showAnnotations = false;

  constructor(
    private investmentPortfolio: InvestmentPortfolio,
    private investmentTransactions: InvestmentTransactions,
    private route: ActivatedRoute,
    private router: Router,
    private themeService: ThemeService,
    private accountService: AccountList,
    private cookieService: CookieService,
    private datePipe: DatePipe,
  ) {
    this.historyChartOptions = {};
  }

  ngOnInit(): void {
    this.theme = this.themeService.theme();
    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;
    });

    //this.title = this.commonControllerServices.getPageTitle(this.router.routerState.snapshot.root);

    this.route.params.subscribe(params => {
      if (params['id']) {
        this.securityId = params['id'];
      }
    });

    const showClosedState = this.getCookie(InvestmentPortfolioComponent.COOKIE_SHOW_CLOSED);

    if (showClosedState !== undefined) {
      this.includeClosedPositions = showClosedState.toLowerCase() === 'true';
    }

    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const ptr = this;

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

            let date = this.datePipe.transform(hoverXaxis);
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
        // position: 'back' - this stopped working when upgrade of ApexCharts....
      },
    };

    this.load();
  }

  onChangeHolding(event: any): void {
    if (this.securityId) {
      this.holding = this.getHolding(this.securityId);
      this.loadTransactions(this.securityId);
      this.loadHistory();
    }
  }

  onChangePositionsSwitch(event: any): void {
    if (this.securityId) {
      this.loadTransactions(this.securityId);
      this.loadHistory();
    }
  }

  translateType(value: string): string {
    if (value === 'BUY') {
      return 'Buy';
    } else if (value === 'SELL') {
      return 'Sell';
    } else if (value === 'REINVEST_DIVIDEND_COMBINED') {
      return 'Reinvestment';
    } else if (value === 'DIVIDEND') {
      return 'Dividend';
    }

    return value;
  }

  abs(value: number): number {
    return Math.abs(value);
  }

  currencyFormatter(value: any): string {
    let code: string | undefined;
    if (this.holding !== null) {
      code = this.holding.currencyCode;
    }
    if (code === undefined) {
      // TODO Fix hard coding here
      code = 'AUD';
    }

    //const val = this.currencyPipe.transform( value, code, 'symbol-narrow');
    const val = formatCurrency(value, '$', code);
    if (val === null) {
      // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
      return '' + value;
    }

    return val;
  }

  getHolding(id: string): FinanceSecurityHolding | null {
    let val = null;
    this.holdings?.every(element => {
      if (element.id === id) {
        val = element;
        return false;
      }
      return true;
    });
    return val;
  }

  load(): void {
    this.investmentPortfolio.get(null, this.includeClosedPositions).subscribe({
      next: (res: FinanceSecurityHolding[]) => {
        this.isLoading = false;
        this.holdings = res;
        if (this.securityId) {
          this.holding = this.getHolding(this.securityId);
          this.loadTransactions(this.securityId);
          this.loadHistory();
        }
      },
      error: () => (this.isLoading = false),
    });
  }

  loadTransactions(id: string): void {
    this.isLoading = true;
    this.isSummaryLoading = true;
    this.investmentTransactions.getTransactions(id, this.includeClosedPositions).subscribe({
      next: (res: InvestmentTransaction[]) => {
        this.isLoading = false;
        this.transactions = res;
        this.loadSummary(id);
      },
      error: () => (this.isLoading = false),
    });
  }

  // loadTransactionsPlus(id: string):void {
  //   this.investmentTransactions.getTransactionsPlus(id, this.includeClosedPositions).subscribe({
  //     next: (res: InvestmentDetails) => {
  //       this.isLoading = false;
  //       this.transactions = res.transactions;
  //       //this.investment = res;
  //       this.loadSummary(id);
  //     },
  //     error: () => (this.isLoading = false),
  //   });
  // }

  loadSummary(id: string): void {
    this.isSummaryLoading = true;
    this.investmentTransactions.getSummary(id, this.includeClosedPositions).subscribe({
      next: (res: FinanceInvestmentSnapshotDetails) => {
        this.isSummaryLoading = false;
        this.investmentSummary = res;
        this.marketValue = Math.round((this.investmentSummary.price * this.investmentSummary.quantity + Number.EPSILON) * 100) / 100;
      },
      error: () => (this.isSummaryLoading = false),
    });
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

  getCookie(key: string): any {
    return this.cookieService.get(key);
  }

  saveCookie(key: string, value: any): void {
    return this.cookieService.put(key, value);
  }

  get onlyTrades(): InvestmentTransaction[] {
    if (this.transactions) {
      return this.transactions.filter(x => x.type === 'BUY' || x.type === 'SELL' || x.type === 'REINVEST_DIVIDEND_COMBINED');
    }
    return [];
  }

  get onlyIncome(): InvestmentTransaction[] {
    if (this.transactions) {
      return this.transactions.filter(x => x.type === 'REINVEST_DIVIDEND_COMBINED' || x.type === 'DIVIDEND');
    }
    return [];
  }

  //
  // Chart / History
  //

  // History / Over-time
  loadHistory(): void {
    if (this.securityId == null) {
      return;
    }
    // eslint-disable-next-line no-console
    console.log('Loading History');
    this.isLoadingHistory = true;

    this.investmentTransactions.getHistory(this.securityId!, this.includeClosedPositions, this.getPeriodString()).subscribe({
      next: (res: FinanceResourceSnapshots[]) => {
        // eslint-disable-next-line no-console
        console.log('Done Loading History');
        this.isLoadingHistory = false;
        this.portfolioValueHistoryItems = res;
        //this.historyChartOptions.series = this.configureHistorySeriesPerHolding(this.portfolioValueHistoryItems!, 'xy');

        this.historyChartOptions.series = this.configureHistorySeriesPerHolding(this.portfolioValueHistoryItems!, 'xy');

        if (this.showAnnotations) {
          this.historyChartOptions.annotations = this.processAnnotations(this.portfolioValueHistoryItems!);
        } else {
          this.historyChartOptions.annotations = {};
        }
      },
      error: () => (this.isLoadingHistory = false),
    });
  }

  private configureHistorySeriesPerHolding(portfolioValueHistoryItems: FinanceResourceSnapshots[], type: string): ApexAxisChartSeries {
    let xydataArr = [{ x: '', y: 0 }];

    let serieses: ApexAxisChartSeries = []; // {data:[0]},{data:[0]}

    for (let i = 0; i < portfolioValueHistoryItems.length; i++) {
      let portfolioValueHistoryItem: FinanceResourceSnapshots = portfolioValueHistoryItems[i];
      xydataArr = [];
      const xyseries = { name: portfolioValueHistoryItem.name, data: xydataArr };
      serieses.push(xyseries);

      portfolioValueHistoryItem.snapshots.forEach((element: { date: string; value: number }) => {
        const xy = { x: '', y: 0 };
        let d = Date.parse(element.date);
        xy.x = this.datePipe.transform(d, 'MM/dd/yyyy')!;
        xy.y = Math.round(element.value);
        xydataArr.push(xy);
      });
    }

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

          xAnnotation.label = { text: element.annotation, style: { color: 'var(--app-colours-primary)', background: bg } };
          xAnnotations.push(xAnnotation);
          // xy.y = Math.round(element.value);
          // xydataArr.push(xy);
        },
      );
    }

    return annotations;
  }

  public isNaN(num: unknown): boolean {
    return Number.isNaN(num);
  }

  private getPeriodString(): string {
    // if(this.selectedPeriod.value == 'ALL') {
    //   return '';
    // } else return this.selectedPeriod.value;
    return '';
  }
  onChangeAnnotationsSwitch(event: any): void {
    if (this.showAnnotations) {
      this.historyChartOptions.annotations = this.processAnnotations(this.portfolioValueHistoryItems!);
    } else {
      this.historyChartOptions.annotations = {};
    }
  }
}
