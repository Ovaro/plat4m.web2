import { Component, Inject, Input, LOCALE_ID, OnChanges, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';
import { SkeletonModule } from 'primeng/skeleton';
import SharedModule from 'app/shared/shared.module';
import { FinancialAccount, PortfolioTrade } from '../finance.model';
import { InvestmentTradeDividendsDialogComponent } from './investment-trade-dividends-dialog.component';
import { InvestmentTradesSummaryDialogComponent } from './investment-trades-summary-dialog.component';
import {
  formatTradeCurrency,
  formatTradeFxRate,
  formatTradePercent,
  getTradeDeltaAmount,
  getTradeDeltaCurrencyCode,
  getTradeDeltaPercent,
  getTradeDividendIncomeAmount,
  getTradeDividendIncomeCurrencyCode,
  getTradeExitFxRate,
  getTradeExitPrice,
  getTradeRowClass,
  hasTradeDividends,
  isReinvestmentTrade,
  TradeCalculationOptions,
} from './investment-trades.util';

@Component({
  selector: 'jhi-investment-trades',
  standalone: true,
  imports: [
    SharedModule,
    RouterModule,
    AutoCompleteModule,
    SkeletonModule,
    InvestmentTradesSummaryDialogComponent,
    InvestmentTradeDividendsDialogComponent,
  ],
  templateUrl: './investment-trades.component.html',
  styleUrls: ['./investment-trades.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class InvestmentTradesComponent implements OnChanges {
  @Input() trades: PortfolioTrade[] = [];
  @Input() isLoadingTrades = false;
  @Input() account: FinancialAccount | null = null;
  @Input() periodAgo = '';
  @Input() periodLabel = 'ALL';

  filteredTrades: PortfolioTrade[] = [];
  visibleTrades: PortfolioTrade[] = [];
  tradeFilterQuery = '';
  tradeFilterSuggestions: string[] = [];
  tradeVisibleCount = 50;
  tradeSortField: 'date' | 'symbol' | 'name' | 'delta' = 'date';
  tradeSortDirection: 'asc' | 'desc' = 'desc';
  tradeIncludeFx = true;
  tradeIncludeReinvestments = true;
  tradeIncludeIncome = false;
  tradeSummaryDialogVisible = false;
  tradeDividendDialogVisible = false;
  selectedTradeDividendTrade: PortfolioTrade | null = null;

  readonly tradeSortOptions = [
    { name: 'Date', value: 'date' },
    { name: 'Symbol', value: 'symbol' },
    { name: 'Name', value: 'name' },
    { name: 'Return', value: 'delta' },
  ];

  constructor(@Inject(LOCALE_ID) private locale: string) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['trades'] || changes['periodAgo']) {
      this.tradeVisibleCount = 50;
      this.refreshTradeSuggestions();
      this.refreshTradeView();
    }
  }

  onTradeFilterComplete(event: AutoCompleteCompleteEvent): void {
    const query = (event.query ?? '').trim().toLowerCase();
    const suggestions = this.buildTradeSuggestionPool();
    this.tradeFilterSuggestions =
      query.length === 0 ? suggestions.slice(0, 12) : suggestions.filter(item => item.toLowerCase().includes(query)).slice(0, 12);
  }

  onTradeFilterChanged(): void {
    this.tradeVisibleCount = 50;
    this.refreshTradeView();
  }

  onTradeSortChanged(): void {
    this.tradeVisibleCount = 50;
    this.refreshTradeView();
  }

  toggleTradeSortDirection(): void {
    this.tradeSortDirection = this.tradeSortDirection === 'desc' ? 'asc' : 'desc';
    this.tradeVisibleCount = 50;
    this.refreshTradeView();
  }

  toggleTradeIncludeFx(): void {
    this.refreshTradeView();
  }

  toggleTradeIncludeReinvestments(): void {
    this.tradeVisibleCount = 50;
    this.refreshTradeView();
  }

  toggleTradeIncludeIncome(): void {
    this.refreshTradeView();
  }

  openTradeDividendDialog(trade: PortfolioTrade, event?: Event): void {
    event?.stopPropagation();
    if (!this.hasTradeDividends(trade)) {
      return;
    }
    this.selectedTradeDividendTrade = trade;
    this.tradeDividendDialogVisible = true;
  }

  openTradesSummary(): void {
    this.tradeSummaryDialogVisible = true;
  }

  loadMoreTrades(): void {
    this.tradeVisibleCount += 50;
    this.refreshTradeView();
  }

  hasMoreTrades(): boolean {
    return this.visibleTrades.length < this.filteredTrades.length;
  }

  getTradeTableColumnCount(): number {
    return 9 + (this.tradeIncludeFx ? 2 : 0) + (this.tradeIncludeIncome ? 1 : 0);
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

  getTradeExitPrice(trade: PortfolioTrade): number | null {
    return getTradeExitPrice(trade);
  }

  getTradeExitFxRate(trade: PortfolioTrade): number | null {
    return getTradeExitFxRate(trade);
  }

  isReinvestmentTrade(trade: PortfolioTrade): boolean {
    return isReinvestmentTrade(trade);
  }

  getReinvestmentTooltip(trade: PortfolioTrade): string {
    return `Dividend reinvestment purchase${trade.type ? ` (${trade.type})` : ''}`;
  }

  getIgnoredTooltip(trade: PortfolioTrade): string {
    const reason = trade.ignoredForRollupReason?.trim();
    return `Ignored in rolled up performance metrics${reason ? `: ${reason}` : '.'}`;
  }

  getClosedTooltip(trade: PortfolioTrade): string {
    return trade.sellDate ? `Closed position. Sold on ${new Date(trade.sellDate).toLocaleDateString(this.locale)}` : 'Closed position';
  }

  getTradeRowClass(trade: PortfolioTrade): string {
    return getTradeRowClass(trade, this.options());
  }

  formatTradeCurrency(value: number | null | undefined, currencyCode: string | null | undefined): string {
    return formatTradeCurrency(value, currencyCode, this.locale);
  }

  formatTradePercent(value: number | null | undefined): string {
    return formatTradePercent(value, this.locale);
  }

  formatTradeFxRate(value: number | null | undefined): string {
    return formatTradeFxRate(value, this.locale);
  }

  formatTradeQuantity(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '—';
    }
    return value.toLocaleString(this.locale, {
      minimumFractionDigits: 0,
      maximumFractionDigits: 4,
    });
  }

  private refreshTradeView(): void {
    const normalizedQuery = this.tradeFilterQuery.trim().toLowerCase();
    const filtered = this.trades.filter(trade => {
      if (!this.tradeIncludeReinvestments && isReinvestmentTrade(trade)) {
        return false;
      }
      if (normalizedQuery.length === 0) {
        return true;
      }
      const haystack = [trade.symbol, trade.name, trade.accountName, trade.type].filter(Boolean).join(' ').toLowerCase();
      return haystack.includes(normalizedQuery);
    });

    filtered.sort((left, right) => this.compareTrades(left, right));
    this.filteredTrades = filtered;
    this.visibleTrades = filtered.slice(0, this.tradeVisibleCount);
  }

  private compareTrades(left: PortfolioTrade, right: PortfolioTrade): number {
    let result = 0;
    if (this.tradeSortField === 'symbol') {
      result = (left.symbol ?? '').localeCompare(right.symbol ?? '', undefined, { sensitivity: 'base' });
    } else if (this.tradeSortField === 'name') {
      result = (left.name ?? '').localeCompare(right.name ?? '', undefined, { sensitivity: 'base' });
    } else if (this.tradeSortField === 'delta') {
      result = (this.getTradeDeltaAmount(left) ?? Number.NEGATIVE_INFINITY) - (this.getTradeDeltaAmount(right) ?? Number.NEGATIVE_INFINITY);
    } else {
      result = (new Date(left.date).getTime() || 0) - (new Date(right.date).getTime() || 0);
    }

    return this.tradeSortDirection === 'asc' ? result : result * -1;
  }

  private refreshTradeSuggestions(): void {
    this.tradeFilterSuggestions = this.buildTradeSuggestionPool().slice(0, 12);
  }

  private buildTradeSuggestionPool(): string[] {
    const values = new Set<string>();
    for (const trade of this.trades) {
      if (!this.tradeIncludeReinvestments && isReinvestmentTrade(trade)) {
        continue;
      }
      if (trade.symbol) {
        values.add(trade.symbol);
      }
      if (trade.name) {
        values.add(trade.name);
      }
      if (trade.accountName) {
        values.add(trade.accountName);
      }
    }
    return Array.from(values);
  }

  private options(): TradeCalculationOptions {
    return {
      includeFx: this.tradeIncludeFx,
      includeIncome: this.tradeIncludeIncome,
      includeReinvestments: this.tradeIncludeReinvestments,
      account: this.account,
      locale: this.locale,
      periodAgo: this.periodAgo,
    };
  }
}
