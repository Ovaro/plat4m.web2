import { Component, EventEmitter, Inject, Input, LOCALE_ID, Output } from '@angular/core';
import { DialogModule } from 'primeng/dialog';
import SharedModule from 'app/shared/shared.module';
import { FinancialAccount, PortfolioTrade, PortfolioTradeDividend } from '../finance.model';
import {
  formatTradeCurrency,
  formatTradeFxRate,
  formatTradeSummaryCurrency,
  getFilteredTradeDividends,
  getTradeDividendIncomeAmount,
  getTradeDividendIncomeCurrencyCode,
  TradeCalculationOptions,
} from './investment-trades.util';

interface DividendDialogEntry {
  trade: PortfolioTrade;
  dividend: PortfolioTradeDividend;
}

interface DividendDialogRow {
  trade: PortfolioTrade;
  dividend: PortfolioTradeDividend;
  allocatedAmount: number | null;
  allocatedSourceAmount: number | null;
  transactionAmount: number | null;
  transactionSourceAmount: number | null;
  allocationRatio: number | null;
}

@Component({
  selector: 'jhi-investment-trade-dividends-dialog',
  standalone: true,
  imports: [SharedModule, DialogModule],
  templateUrl: './investment-trade-dividends-dialog.component.html',
})
export class InvestmentTradeDividendsDialogComponent {
  @Input() visible = false;
  @Input() trade: PortfolioTrade | null = null;
  @Input() trades: PortfolioTrade[] | null = null;
  @Input() includeFx = true;
  @Input() includeIncome = false;
  @Input() account: FinancialAccount | null = null;
  @Input() periodAgo = '';
  @Input() periodLabel = 'ALL';

  @Output() visibleChange = new EventEmitter<boolean>();

  constructor(@Inject(LOCALE_ID) private locale: string) {}

  get header(): string {
    return this.trade ? `Dividends for ${this.trade.symbol}` : 'Dividend Income Breakdown';
  }

  get dividendEntries(): DividendDialogEntry[] {
    const sourceTrades = this.trade ? [this.trade] : (this.trades ?? []);

    return sourceTrades
      .flatMap(trade => this.getFilteredTradeDividends(trade).map(dividend => ({ trade, dividend })))
      .sort((left, right) => new Date(right.dividend.date).getTime() - new Date(left.dividend.date).getTime());
  }

  get dividendRows(): DividendDialogRow[] {
    const rows = this.dividendEntries.map(entry => this.toDividendRow(entry));
    if (!this.hasMultipleTrades) {
      return rows;
    }

    const groupedRows = new Map<string, DividendDialogRow>();
    for (const row of rows) {
      const key = this.dividendGroupKey(row);
      const existingRow = groupedRows.get(key);
      if (!existingRow) {
        groupedRows.set(key, { ...row });
        continue;
      }

      existingRow.allocatedAmount = this.sumNullable(existingRow.allocatedAmount, row.allocatedAmount);
      existingRow.allocatedSourceAmount = this.sumNullable(existingRow.allocatedSourceAmount, row.allocatedSourceAmount);
      existingRow.allocationRatio = this.sumNullable(existingRow.allocationRatio, row.allocationRatio);
      existingRow.transactionAmount ??= row.transactionAmount;
      existingRow.transactionSourceAmount ??= row.transactionSourceAmount;
    }

    return Array.from(groupedRows.values()).sort(
      (left, right) =>
        new Date(right.dividend.date).getTime() - new Date(left.dividend.date).getTime() ||
        (left.trade.symbol ?? '').localeCompare(right.trade.symbol ?? '', undefined, { sensitivity: 'base' }),
    );
  }

  get hasMultipleTrades(): boolean {
    return !this.trade && (this.trades?.length ?? 0) > 0;
  }

  get selectedDividendTradeCount(): number {
    return new Set(this.dividendEntries.map(entry => entry.trade.id)).size;
  }

  onVisibleChange(value: boolean): void {
    this.visible = value;
    this.visibleChange.emit(value);
  }

  getSelectedTradeDividendTotal(): number | null {
    if (this.trade) {
      return getTradeDividendIncomeAmount(this.trade, this.options());
    }
    if (this.dividendRows.length === 0) {
      return 0;
    }

    return this.dividendRows.reduce((sum, row) => sum + (row.allocatedAmount ?? 0), 0);
  }

  getTradeDividendIncomeCurrencyCode(trade: PortfolioTrade): string {
    return getTradeDividendIncomeCurrencyCode(trade, this.options());
  }

  formatTradeCurrency(value: number | null | undefined, currencyCode: string | null | undefined): string {
    return formatTradeCurrency(value, currencyCode, this.locale);
  }

  formatTradeFxRate(value: number | null | undefined): string {
    return formatTradeFxRate(value, this.locale);
  }

  formatAllocationRatio(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }
    return `${value.toLocaleString(this.locale, { style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  formatTradeSummaryCurrency(value: number | null | undefined): string {
    return formatTradeSummaryCurrency(value, this.options());
  }

  getFilteredTradeDividends(trade: PortfolioTrade): PortfolioTradeDividend[] {
    return getFilteredTradeDividends(trade, this.options());
  }

  getDividendDisplayCurrencyCode(row: DividendDialogRow): string {
    return this.includeFx
      ? (row.dividend.baseCurrencyCode ?? row.trade.currencyCode ?? 'AUD')
      : (row.dividend.sourceCurrencyCode ?? row.trade.currencyCode ?? 'AUD');
  }

  private toDividendRow(entry: DividendDialogEntry): DividendDialogRow {
    const allocatedAmount = this.includeFx ? entry.dividend.amountWithFx : entry.dividend.amountWithoutFx;
    const transactionAmount = this.includeFx ? entry.dividend.transactionAmountWithFx : entry.dividend.transactionAmountWithoutFx;

    return {
      trade: entry.trade,
      dividend: entry.dividend,
      allocatedAmount: allocatedAmount ?? null,
      allocatedSourceAmount: entry.dividend.sourceAmount ?? null,
      transactionAmount: transactionAmount ?? allocatedAmount ?? null,
      transactionSourceAmount: entry.dividend.transactionSourceAmount ?? entry.dividend.sourceAmount ?? null,
      allocationRatio: entry.dividend.allocationRatio ?? null,
    };
  }

  private dividendGroupKey(row: DividendDialogRow): string {
    return (
      row.dividend.id ??
      [
        row.trade.accountId,
        row.trade.securityId,
        row.dividend.date,
        row.dividend.type,
        row.transactionAmount ?? '',
        row.transactionSourceAmount ?? '',
      ].join('|')
    );
  }

  private sumNullable(left: number | null, right: number | null): number | null {
    if (left === null && right === null) {
      return null;
    }
    return (left ?? 0) + (right ?? 0);
  }

  private options(): TradeCalculationOptions {
    return {
      includeFx: this.includeFx,
      includeIncome: this.includeIncome,
      includeReinvestments: true,
      account: this.account,
      locale: this.locale,
      periodAgo: this.periodAgo,
    };
  }
}
