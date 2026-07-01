import { Component, Inject, LOCALE_ID, ViewChild, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ThemeChangeEvent, ThemeService } from 'app/layouts/main/theme.service';
import { InvestmentTransactions } from './investment.service';
import {
  FinanceInvestmentSnapshotDetails,
  FinanceResourceSnapshots,
  FinanceSecurityHolding,
  FinanceSecurityStoredPrice,
  InvestmentTransaction,
} from '../finance.model';
import { formatCurrency, formatDate } from '@angular/common';
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
import { DialogModule } from 'primeng/dialog';

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
  imports: [SharedModule, NgApexchartsModule, DialogModule, ReactiveFormsModule],
})
export class InvestmentComponent {
  // implements OnInit
  private readonly _title = signal('');
  private readonly _theme = signal('light');
  private readonly _isLoading = signal(false);
  private readonly _isSummaryLoading = signal(false);
  private readonly _isLoadingHistory = signal(false);
  private readonly _isRefreshingQuotes = signal(false);
  private readonly _securityId = signal<string | null>(null);
  private readonly _holding = signal<FinanceSecurityHolding | null>(null);
  private readonly _includeClosedPositions = signal(false);
  private readonly _holdings = signal<FinanceSecurityHolding[] | null>(null);
  private readonly _investmentSummary = signal<FinanceInvestmentSnapshotDetails | null>(null);
  private readonly _marketValue = signal(0);
  private readonly _transactions = signal<InvestmentTransaction[] | null>(null);
  private readonly _portfolioValueHistoryItems = signal<FinanceResourceSnapshots[] | null>(null);
  private readonly _showAnnotations = signal(false);
  private readonly _showHistoryChart = signal(false);
  private readonly _quoteRefreshMessage = signal<string | null>(null);
  protected actionsMenuOpen = false;
  protected priceDialogVisible = false;
  protected priceDeleteDialogVisible = false;
  protected isPricesLoading = false;
  protected isPriceSaving = false;
  protected priceErrorMessage: string | null = null;
  protected storedPrices: FinanceSecurityStoredPrice[] = [];
  protected editingStoredPrice: FinanceSecurityStoredPrice | null = null;
  protected deleteStoredPriceCandidate: FinanceSecurityStoredPrice | null = null;
  protected readonly priceForm = new FormGroup({
    date: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    price: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(0)] }),
  });

  //public investmen2!: InvestmentDetails | null;
  @ViewChild('historyChart', { static: false }) historyChart!: ChartComponent;
  public historyChartOptions: Partial<AreaChartOptions>;
  readonly emptyHistorySeries: ApexAxisChartSeries = [{ name: 'History', data: [] }];

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

  get isSummaryLoading(): boolean {
    return this._isSummaryLoading();
  }

  set isSummaryLoading(value: boolean) {
    this._isSummaryLoading.set(value);
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

  get securityId(): string | null {
    return this._securityId();
  }

  set securityId(value: string | null) {
    this._securityId.set(value);
  }

  get holding(): FinanceSecurityHolding | null {
    return this._holding();
  }

  set holding(value: FinanceSecurityHolding | null) {
    this._holding.set(value);
  }

  get includeClosedPositions(): boolean {
    return this._includeClosedPositions();
  }

  set includeClosedPositions(value: boolean) {
    this._includeClosedPositions.set(value);
  }

  get holdings(): FinanceSecurityHolding[] | null {
    return this._holdings();
  }

  set holdings(value: FinanceSecurityHolding[] | null) {
    this._holdings.set(value);
  }

  get investmentSummary(): FinanceInvestmentSnapshotDetails | null {
    return this._investmentSummary();
  }

  set investmentSummary(value: FinanceInvestmentSnapshotDetails | null) {
    this._investmentSummary.set(value);
  }

  get marketValue(): number {
    return this._marketValue();
  }

  set marketValue(value: number) {
    this._marketValue.set(value);
  }

  get transactions(): InvestmentTransaction[] | null {
    return this._transactions();
  }

  set transactions(value: InvestmentTransaction[] | null) {
    this._transactions.set(value);
  }

  get portfolioValueHistoryItems(): FinanceResourceSnapshots[] | null {
    return this._portfolioValueHistoryItems();
  }

  set portfolioValueHistoryItems(value: FinanceResourceSnapshots[] | null) {
    this._portfolioValueHistoryItems.set(value);
  }

  get showAnnotations(): boolean {
    return this._showAnnotations();
  }

  set showAnnotations(value: boolean) {
    this._showAnnotations.set(value);
  }

  get showHistoryChart(): boolean {
    return this._showHistoryChart();
  }

  set showHistoryChart(value: boolean) {
    this._showHistoryChart.set(value);
  }

  get quoteRefreshMessage(): string | null {
    return this._quoteRefreshMessage();
  }

  set quoteRefreshMessage(value: string | null) {
    this._quoteRefreshMessage.set(value);
  }

  constructor(
    private investmentPortfolio: InvestmentPortfolio,
    private investmentTransactions: InvestmentTransactions,
    private route: ActivatedRoute,
    private router: Router,
    private themeService: ThemeService,
    private accountService: AccountList,
    private cookieService: CookieService,
    @Inject(LOCALE_ID) private locale: string,
  ) {
    this.historyChartOptions = {
      series: this.emptyHistorySeries,
      annotations: {},
    };
  }

  ngOnInit(): void {
    this.theme = this.themeService.theme();
    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;
      if (this.historyChart) {
        this.historyChart.updateOptions({ tooltip: { theme: this.theme } });
      }
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
      series: this.emptyHistorySeries,
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
    const val = formatCurrency(value, 'en-AU', '$', 'AUD');
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
        this.title = this.holding?.name ?? '';
        this.marketValue = Math.round((this.investmentSummary.price * this.investmentSummary.quantity + Number.EPSILON) * 100) / 100;
      },
      error: () => (this.isSummaryLoading = false),
    });
  }

  refreshQuotes(): void {
    if (this.isRefreshingQuotes || this.securityId == null) {
      return;
    }

    this.isRefreshingQuotes = true;
    this.quoteRefreshMessage = null;
    this.investmentTransactions.refreshQuotes(this.securityId).subscribe({
      next: result => {
        this.isRefreshingQuotes = false;
        this.quoteRefreshMessage = `Updated ${result.refreshedCount} holding${result.refreshedCount === 1 ? '' : 's'}.`;
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.isRefreshingQuotes = false;
        this.quoteRefreshMessage = this.getQuoteRefreshErrorMessage(error);
      },
    });
  }

  openPriceDialog(): void {
    if (!this.securityId) {
      return;
    }
    this.actionsMenuOpen = false;
    this.priceDialogVisible = true;
    this.resetStoredPriceForm();
    this.loadStoredPrices();
  }

  toggleActionsMenu(): void {
    this.actionsMenuOpen = !this.actionsMenuOpen;
  }

  refreshQuotesFromActionsMenu(): void {
    this.actionsMenuOpen = false;
    this.refreshQuotes();
  }

  resetStoredPriceForm(): void {
    this.editingStoredPrice = null;
    this.priceErrorMessage = null;
    this.priceForm.reset({
      date: formatDate(new Date(), 'yyyy-MM-dd', this.locale),
      price: null,
    });
  }

  startAddingStoredPrice(): void {
    this.resetStoredPriceForm();
  }

  selectStoredPrice(price: FinanceSecurityStoredPrice): void {
    this.editingStoredPrice = price;
    this.priceErrorMessage = null;
    this.priceForm.reset({
      date: price.date.slice(0, 10),
      price: price.price,
    });
  }

  confirmDeleteStoredPrice(price: FinanceSecurityStoredPrice): void {
    this.deleteStoredPriceCandidate = price;
    this.priceDeleteDialogVisible = true;
  }

  saveStoredPrice(): void {
    if (!this.securityId || this.priceForm.invalid) {
      this.priceForm.markAllAsTouched();
      return;
    }

    const updateExisting = this.editingStoredPrice !== null;
    const update = {
      date: this.priceForm.controls.date.value,
      price: this.priceForm.controls.price.value!,
      comment: updateExisting ? (this.editingStoredPrice?.comment ?? null) : null,
    };

    this.isPriceSaving = true;
    this.priceErrorMessage = null;
    const request =
      updateExisting && this.editingStoredPrice
        ? this.investmentTransactions.updateStoredPrice(this.securityId, this.editingStoredPrice.id, update)
        : this.investmentTransactions.createStoredPrice(this.securityId, update);

    request.subscribe({
      next: savedPrice => {
        this.isPriceSaving = false;
        this.selectStoredPrice(savedPrice);
        this.loadStoredPrices();
        this.reloadAfterStoredPriceChange();
      },
      error: (error: HttpErrorResponse) => {
        this.isPriceSaving = false;
        this.priceErrorMessage = this.getQuoteRefreshErrorMessage(error);
      },
    });
  }

  deleteStoredPrice(): void {
    if (!this.securityId || !this.deleteStoredPriceCandidate) {
      return;
    }

    const deletedPriceId = this.deleteStoredPriceCandidate.id;
    this.isPriceSaving = true;
    this.priceErrorMessage = null;
    this.investmentTransactions.deleteStoredPrice(this.securityId, this.deleteStoredPriceCandidate.id).subscribe({
      next: () => {
        this.isPriceSaving = false;
        if (this.editingStoredPrice?.id === deletedPriceId) {
          this.resetStoredPriceForm();
        }
        this.deleteStoredPriceCandidate = null;
        this.priceDeleteDialogVisible = false;
        this.loadStoredPrices();
        this.reloadAfterStoredPriceChange();
      },
      error: (error: HttpErrorResponse) => {
        this.isPriceSaving = false;
        this.priceErrorMessage = this.getQuoteRefreshErrorMessage(error);
      },
    });
  }

  private loadStoredPrices(): void {
    if (!this.securityId) {
      return;
    }
    this.isPricesLoading = true;
    this.priceErrorMessage = null;
    this.investmentTransactions.getStoredPrices(this.securityId).subscribe({
      next: prices => {
        this.storedPrices = prices;
        this.isPricesLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.priceErrorMessage = this.getQuoteRefreshErrorMessage(error);
        this.isPricesLoading = false;
      },
    });
  }

  private reloadAfterStoredPriceChange(): void {
    if (!this.securityId) {
      return;
    }
    this.loadSummary(this.securityId);
    this.loadHistory();
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
        this.setHistorySeries(this.configureHistorySeriesPerHolding(this.portfolioValueHistoryItems!, 'xy'));

        if (this.showAnnotations) {
          this.setHistoryAnnotations(this.processAnnotations(this.portfolioValueHistoryItems!));
        } else {
          this.setHistoryAnnotations({});
        }
        this.refreshHistoryChart();
      },
      error: () => ((this.isLoadingHistory = false), (this.showHistoryChart = false)),
    });
  }

  private configureHistorySeriesPerHolding(portfolioValueHistoryItems: FinanceResourceSnapshots[], type: string): ApexAxisChartSeries {
    let xydataArr = [{ x: 0, y: 0, time: 0 }];

    let serieses: ApexAxisChartSeries = []; // {data:[0]},{data:[0]}

    for (let i = 0; i < portfolioValueHistoryItems.length; i++) {
      let portfolioValueHistoryItem: FinanceResourceSnapshots = portfolioValueHistoryItems[i];
      xydataArr = [];
      const xyseries = { name: portfolioValueHistoryItem.name, data: xydataArr };
      serieses.push(xyseries);

      portfolioValueHistoryItem.snapshots?.forEach((element: { date: string; value: number }) => {
        let d = Date.parse(element.date);
        if (Number.isNaN(d)) {
          return;
        }
        const xy = { x: d, y: Math.round(element.value), time: d };
        xydataArr.push(xy);
      });
    }

    return this.normalizeAxisSeries(serieses, 'History');
  }

  private processAnnotations(portfolioValueHistoryItems: FinanceResourceSnapshots[]): ApexAnnotations {
    let xAnnotations: XAxisAnnotations[] = [];
    let annotations: ApexAnnotations = { xaxis: xAnnotations };

    for (let i = 0; i < portfolioValueHistoryItems.length; i++) {
      let portfolioValueHistoryItem: FinanceResourceSnapshots = portfolioValueHistoryItems[i];

      portfolioValueHistoryItem.annotations?.forEach(
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
      this.setHistoryAnnotations(this.processAnnotations(this.portfolioValueHistoryItems!));
    } else {
      this.setHistoryAnnotations({});
    }
    this.refreshHistoryChart();
  }

  private setHistorySeries(series: ApexAxisChartSeries): void {
    this.historyChartOptions = {
      ...this.historyChartOptions,
      series: this.normalizeAxisSeries(series, 'History'),
    };
  }

  private setHistoryAnnotations(annotations: ApexAnnotations): void {
    this.historyChartOptions = {
      ...this.historyChartOptions,
      annotations,
    };
  }

  private normalizeAxisSeries(series: ApexAxisChartSeries | null | undefined, defaultName: string): ApexAxisChartSeries {
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
      }));

    if (normalizedSeries.length === 0) {
      return [{ name: defaultName, data: [] }];
    }

    return normalizedSeries;
  }

  private refreshHistoryChart(): void {
    this.showHistoryChart = false;
    queueMicrotask(() => {
      this.showHistoryChart = true;
    });
  }
}
