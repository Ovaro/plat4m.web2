import { Component, EventEmitter, Inject, Input, LOCALE_ID, OnChanges, Output, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { RouterModule } from '@angular/router';
import { DialogModule } from 'primeng/dialog';
import SharedModule from 'app/shared/shared.module';
import { FinancialAccount, PortfolioTrade } from '../finance.model';
import { InvestmentTradeDividendsDialogComponent } from './investment-trade-dividends-dialog.component';
import {
  bestHoldingBucketLabel,
  cagr,
  emptyTradeSummaryYear,
  formatTradeCurrency,
  formatTradeFxRate,
  formatTradeNumber,
  formatTradePercent,
  formatTradeSummaryCurrency,
  getTradeCapitalInvested,
  getTradeDeltaAmount,
  getTradeDeltaCurrencyCode,
  getTradeDeltaPercent,
  getTradeDividendIncomeAmount,
  getTradeDividendIncomeCurrencyCode,
  getTradeExitPrice,
  getTradeHoldingYears,
  getTradeStatus,
  hasTradeDividends,
  holdingBucketLabel,
  HoldingBucketSummary,
  includedTrades,
  safePercent,
  TradeCalculationOptions,
  TradeSummaryTotals,
  TradeSummaryYear,
} from './investment-trades.util';

@Component({
  selector: 'jhi-investment-trades-summary-dialog',
  standalone: true,
  imports: [SharedModule, RouterModule, DialogModule, InvestmentTradeDividendsDialogComponent],
  templateUrl: './investment-trades-summary-dialog.component.html',
  styleUrls: ['./investment-trades.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class InvestmentTradesSummaryDialogComponent implements OnChanges {
  @Input() visible = false;
  @Input() trades: PortfolioTrade[] = [];
  @Input() account: FinancialAccount | null = null;
  @Input() includeFx = true;
  @Input() includeReinvestments = true;
  @Input() includeIncome = false;
  @Input() periodAgo = '';
  @Input() periodLabel = 'ALL';

  @Output() visibleChange = new EventEmitter<boolean>();

  tradeSummaryYearDialogVisible = false;
  tradeDividendDialogVisible = false;
  selectedTradeDividendTrade: PortfolioTrade | null = null;
  selectedTradeDividendTrades: PortfolioTrade[] = [];
  selectedTradeSummaryYear: TradeSummaryYear | null = null;
  selectedTradeSummaryPositiveTrades: PortfolioTrade[] = [];
  selectedTradeSummaryNegativeTrades: PortfolioTrade[] = [];
  selectedTradeSummaryNeutralTrades: PortfolioTrade[] = [];
  tradeSummaryYears: TradeSummaryYear[] = [];
  tradeSummaryTotals: TradeSummaryTotals | null = null;
  tradeHoldingBuckets: HoldingBucketSummary[] = [];
  selectedSummaryYearRange: number | null = 10;

  readonly summaryYearRangeOptions: { label: string; years: number | null }[] = [
    { label: '1y', years: 1 },
    { label: '2y', years: 2 },
    { label: '3y', years: 3 },
    { label: '4y', years: 4 },
    { label: '5y', years: 5 },
    { label: '7y', years: 7 },
    { label: '10y', years: 10 },
    { label: '15y', years: 15 },
    { label: 'ALL', years: null },
  ];

  constructor(@Inject(LOCALE_ID) private locale: string) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (
      this.visible ||
      changes['trades'] ||
      changes['includeFx'] ||
      changes['includeReinvestments'] ||
      changes['includeIncome'] ||
      changes['account'] ||
      changes['periodAgo']
    ) {
      this.buildTradeSummary();
    }
    if (this.tradeSummaryYearDialogVisible && this.selectedTradeSummaryYear) {
      this.openTradeSummaryYear(this.selectedTradeSummaryYear);
    }
  }

  onVisibleChange(value: boolean): void {
    this.visible = value;
    this.visibleChange.emit(value);
    if (value) {
      this.buildTradeSummary();
    }
  }

  openTradeDividendDialog(trade: PortfolioTrade, event?: Event): void {
    event?.stopPropagation();
    if (!this.hasTradeDividends(trade)) {
      return;
    }
    this.selectedTradeDividendTrade = trade;
    this.selectedTradeDividendTrades = [];
    this.tradeDividendDialogVisible = true;
  }

  openSummaryDividendDialog(): void {
    const tradesWithDividends = this.summaryTrades().filter(trade => this.hasTradeDividends(trade));
    if (tradesWithDividends.length === 0) {
      return;
    }
    this.selectedTradeDividendTrade = null;
    this.selectedTradeDividendTrades = tradesWithDividends;
    this.tradeDividendDialogVisible = true;
  }

  hasSummaryDividends(): boolean {
    return this.summaryTrades().some(trade => this.hasTradeDividends(trade));
  }

  selectSummaryYearRange(years: number | null): void {
    if (this.selectedSummaryYearRange === years) {
      return;
    }
    this.selectedSummaryYearRange = years;
    this.buildTradeSummary();
    if (this.tradeSummaryYearDialogVisible && this.selectedTradeSummaryYear) {
      this.openTradeSummaryYear(this.selectedTradeSummaryYear);
    }
  }

  get selectedSummaryYearRangeLabel(): string {
    return this.summaryYearRangeOptions.find(option => option.years === this.selectedSummaryYearRange)?.label ?? '10y';
  }

  openTradeSummaryYear(year: TradeSummaryYear): void {
    this.selectedTradeSummaryYear = year;
    const tradesForYear = this.summaryTrades()
      .filter(trade => {
        const date = new Date(trade.date);
        return !Number.isNaN(date.getTime()) && date.getFullYear() === year.year && this.getTradeDeltaAmount(trade) !== null;
      })
      .sort((left, right) => (this.getTradeDeltaAmount(right) ?? 0) - (this.getTradeDeltaAmount(left) ?? 0));

    this.selectedTradeSummaryPositiveTrades = tradesForYear.filter(trade => (this.getTradeDeltaAmount(trade) ?? 0) > 0);
    this.selectedTradeSummaryNegativeTrades = tradesForYear.filter(trade => (this.getTradeDeltaAmount(trade) ?? 0) < 0);
    this.selectedTradeSummaryNeutralTrades = tradesForYear.filter(trade => (this.getTradeDeltaAmount(trade) ?? 0) === 0);
    this.tradeSummaryYearDialogVisible = true;
  }

  getTradeDetailColumnCount(): number {
    return 12 + (this.includeIncome ? 1 : 0);
  }

  getSelectedTradeDividendTotal(): number | null {
    return this.selectedTradeDividendTrade ? this.getTradeDividendIncomeAmount(this.selectedTradeDividendTrade) : null;
  }

  getTradeDeltaAmount(trade: PortfolioTrade): number | null {
    return getTradeDeltaAmount(trade, this.options());
  }

  getTradeDeltaPercent(trade: PortfolioTrade): number | null {
    return getTradeDeltaPercent(trade, this.options());
  }

  getTradeDeltaCurrencyCode(trade: PortfolioTrade): string {
    return getTradeDeltaCurrencyCode(trade, this.options());
  }

  getTradeDividendIncomeAmount(trade: PortfolioTrade): number | null {
    return getTradeDividendIncomeAmount(trade, this.options());
  }

  getTradeDividendIncomeCurrencyCode(trade: PortfolioTrade): string {
    return getTradeDividendIncomeCurrencyCode(trade, this.options());
  }

  hasTradeDividends(trade: PortfolioTrade): boolean {
    return hasTradeDividends(trade, this.options());
  }

  getTradeCapitalInvested(trade: PortfolioTrade): number | null {
    return getTradeCapitalInvested(trade, this.options());
  }

  getTradeExitPrice(trade: PortfolioTrade): number | null {
    return getTradeExitPrice(trade);
  }

  getTradeStatus(trade: PortfolioTrade): string {
    return getTradeStatus(trade, this.locale);
  }

  getTradeHoldingYears(trade: PortfolioTrade): number | null {
    return getTradeHoldingYears(trade);
  }

  formatTradeSummaryCurrency(value: number | null | undefined): string {
    return formatTradeSummaryCurrency(value, this.options());
  }

  formatTradeCurrency(value: number | null | undefined, currencyCode: string | null | undefined): string {
    return formatTradeCurrency(value, currencyCode, this.locale);
  }

  formatTradePercent(value: number | null | undefined): string {
    return formatTradePercent(value, this.locale);
  }

  formatTradeNumber(value: number | null | undefined, digits = '1.1-1'): string {
    return formatTradeNumber(value, this.locale, digits);
  }

  formatTradeFxRate(value: number | null | undefined): string {
    return formatTradeFxRate(value, this.locale);
  }

  private buildTradeSummary(): void {
    const now = new Date();
    const currentYear = now.getFullYear();
    const allSummaryTrades = this.includedTrades();
    const firstTradeYear = this.getFirstTradeYear(allSummaryTrades) ?? currentYear;
    const startYear = this.selectedSummaryYearRange === null ? firstTradeYear : currentYear - this.selectedSummaryYearRange + 1;
    const yearRows = new Map<number, TradeSummaryYear>();

    for (let year = currentYear; year >= startYear; year--) {
      yearRows.set(year, emptyTradeSummaryYear(year));
    }

    const bucketRows = new Map<string, HoldingBucketSummary>(
      ['<1 year', '1-2 years', '2-3 years', '3-5 years', '5+ years'].map(label => [
        label,
        { label, tradeCount: 0, capitalInvested: 0, totalGain: 0, returnPercent: null, positiveTradePercent: null },
      ]),
    );
    const bucketPositiveCounts = new Map<string, number>();

    let totalCapitalInvested = 0;
    let totalGain = 0;
    let totalDividendIncome = 0;
    let positiveTradeCount = 0;
    let negativeTradeCount = 0;
    let positiveCapitalInvested = 0;
    let negativeCapitalInvested = 0;
    let positiveGain = 0;
    let negativeGain = 0;
    let holdingYearsSum = 0;
    let weightedHoldingYearsSum = 0;
    let holdingCapitalWeight = 0;

    const summaryTrades = this.summaryTrades(startYear, currentYear, allSummaryTrades);
    for (const trade of summaryTrades) {
      const date = new Date(trade.date);
      if (Number.isNaN(date.getTime())) {
        continue;
      }
      const capital = this.getTradeCapitalInvested(trade);
      const delta = this.getTradeDeltaAmount(trade);
      if (capital === null || delta === null) {
        continue;
      }

      const holdingYears = this.getTradeHoldingYears(trade);
      if (holdingYears === null) {
        continue;
      }
      const bucket = holdingBucketLabel(holdingYears);
      const bucketRow = bucketRows.get(bucket);
      if (bucketRow) {
        bucketRow.tradeCount++;
        bucketRow.capitalInvested += capital;
        bucketRow.totalGain += delta;
        if (delta > 0) {
          bucketPositiveCounts.set(bucket, (bucketPositiveCounts.get(bucket) ?? 0) + 1);
        }
      }

      totalCapitalInvested += capital;
      totalGain += delta;
      totalDividendIncome += this.getTradeDividendIncomeAmount(trade) ?? 0;
      holdingYearsSum += holdingYears;
      weightedHoldingYearsSum += holdingYears * capital;
      holdingCapitalWeight += capital;

      if (delta > 0) {
        positiveTradeCount++;
        positiveCapitalInvested += capital;
        positiveGain += delta;
      } else if (delta < 0) {
        negativeTradeCount++;
        negativeCapitalInvested += capital;
        negativeGain += delta;
      }

      const row = yearRows.get(date.getFullYear());
      if (row) {
        row.tradeCount++;
        row.capitalInvested += capital;
        row.totalGain += delta;
        if (delta > 0) {
          row.positiveTradeCount++;
          row.positiveCapitalInvested += capital;
          row.positiveGain += delta;
        } else if (delta < 0) {
          row.negativeTradeCount++;
          row.negativeCapitalInvested += capital;
          row.negativeGain += delta;
        }
        row.averageHoldingYears = (row.averageHoldingYears ?? 0) + holdingYears;
      }
    }

    let cumulativeCapital = 0;
    let cumulativePositiveGain = 0;
    let cumulativeNegativeGain = 0;
    const rowsAscending = Array.from(yearRows.values()).sort((left, right) => left.year - right.year);
    for (const row of rowsAscending) {
      cumulativeCapital += row.capitalInvested;
      cumulativePositiveGain += row.positiveGain;
      cumulativeNegativeGain += row.negativeGain;
      row.cumulativeCapitalInvested = cumulativeCapital;
      row.positiveGainCumulative = cumulativePositiveGain;
      row.negativeGainCumulative = cumulativeNegativeGain;
      row.positiveTradePercent = safePercent(row.positiveTradeCount, row.tradeCount);
      row.negativeTradePercent = safePercent(row.negativeTradeCount, row.tradeCount);
      row.positiveCapitalPercent = safePercent(row.positiveCapitalInvested, row.capitalInvested);
      row.negativeCapitalPercent = safePercent(row.negativeCapitalInvested, row.capitalInvested);
      row.positiveGainPercent = safePercent(row.positiveGain, row.capitalInvested);
      row.negativeGainPercent = safePercent(row.negativeGain, row.capitalInvested);
      row.totalGainPercent = safePercent(row.totalGain, row.capitalInvested);
      row.averageHoldingYears = row.tradeCount > 0 && row.averageHoldingYears !== null ? row.averageHoldingYears / row.tradeCount : null;
      row.cagr = cagr(cumulativeCapital, cumulativeCapital + cumulativePositiveGain + cumulativeNegativeGain, row.year - startYear + 1);
    }

    const transactionCount =
      positiveTradeCount + negativeTradeCount + summaryTrades.filter(trade => this.getTradeDeltaAmount(trade) === 0).length;
    const buckets = Array.from(bucketRows.values()).map(bucket => ({
      ...bucket,
      returnPercent: safePercent(bucket.totalGain, bucket.capitalInvested),
      positiveTradePercent: safePercent(bucketPositiveCounts.get(bucket.label) ?? 0, bucket.tradeCount),
    }));

    this.tradeSummaryYears = Array.from(yearRows.values()).sort((left, right) => right.year - left.year);
    this.tradeHoldingBuckets = buckets;
    this.tradeSummaryTotals = {
      totalCapitalInvested,
      totalGain,
      totalDividendIncome,
      transactionCount,
      positiveTradeCount,
      negativeTradeCount,
      positiveTradePercent: safePercent(positiveTradeCount, transactionCount),
      negativeTradePercent: safePercent(negativeTradeCount, transactionCount),
      positiveCapitalInvested,
      negativeCapitalInvested,
      positiveCapitalPercent: safePercent(positiveCapitalInvested, totalCapitalInvested),
      negativeCapitalPercent: safePercent(negativeCapitalInvested, totalCapitalInvested),
      positiveGain,
      negativeGain,
      positiveGainPercent: safePercent(positiveGain, totalCapitalInvested),
      negativeGainPercent: safePercent(negativeGain, totalCapitalInvested),
      averageHoldingYears: transactionCount > 0 ? holdingYearsSum / transactionCount : null,
      weightedHoldingYears: holdingCapitalWeight > 0 ? weightedHoldingYearsSum / holdingCapitalWeight : null,
      bestHoldingBucket: bestHoldingBucketLabel(buckets),
      cagr: cagr(totalCapitalInvested, totalCapitalInvested + totalGain, currentYear - startYear + 1),
    };
  }

  private getFirstTradeYear(trades: PortfolioTrade[]): number | null {
    let firstYear: number | null = null;
    for (const trade of trades) {
      const date = new Date(trade.date);
      if (Number.isNaN(date.getTime())) {
        continue;
      }
      const year = date.getFullYear();
      firstYear = firstYear === null ? year : Math.min(firstYear, year);
    }
    return firstYear;
  }

  private includedTrades(): PortfolioTrade[] {
    return includedTrades(this.trades, this.includeReinvestments);
  }

  private summaryTrades(
    startYear = this.getSummaryStartYear(),
    currentYear = new Date().getFullYear(),
    trades = this.includedTrades(),
  ): PortfolioTrade[] {
    return trades.filter(trade => {
      const date = new Date(trade.date);
      return !Number.isNaN(date.getTime()) && date.getFullYear() >= startYear && date.getFullYear() <= currentYear;
    });
  }

  private getSummaryStartYear(): number {
    const currentYear = new Date().getFullYear();
    if (this.selectedSummaryYearRange !== null) {
      return currentYear - this.selectedSummaryYearRange + 1;
    }
    return this.getFirstTradeYear(this.includedTrades()) ?? currentYear;
  }

  private options(): TradeCalculationOptions {
    return {
      includeFx: this.includeFx,
      includeIncome: this.includeIncome,
      includeReinvestments: this.includeReinvestments,
      account: this.account,
      locale: this.locale,
      periodAgo: this.periodAgo,
    };
  }
}
