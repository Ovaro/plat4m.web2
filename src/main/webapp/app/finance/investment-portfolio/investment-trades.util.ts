import { formatCurrency, getCurrencySymbol } from '@angular/common';
import { FinancialAccount, PortfolioTrade, PortfolioTradeDividend } from '../finance.model';

export interface TradeSummaryYear {
  year: number;
  capitalInvested: number;
  cumulativeCapitalInvested: number;
  tradeCount: number;
  positiveTradeCount: number;
  negativeTradeCount: number;
  positiveTradePercent: number | null;
  negativeTradePercent: number | null;
  positiveCapitalInvested: number;
  negativeCapitalInvested: number;
  positiveCapitalPercent: number | null;
  negativeCapitalPercent: number | null;
  positiveGain: number;
  positiveGainPercent: number | null;
  positiveGainCumulative: number;
  negativeGain: number;
  negativeGainPercent: number | null;
  negativeGainCumulative: number;
  totalGain: number;
  totalGainPercent: number | null;
  averageHoldingYears: number | null;
  cagr: number | null;
}

export interface TradeSummaryTotals {
  totalCapitalInvested: number;
  totalGain: number;
  totalDividendIncome: number;
  transactionCount: number;
  positiveTradeCount: number;
  negativeTradeCount: number;
  positiveTradePercent: number | null;
  negativeTradePercent: number | null;
  positiveCapitalInvested: number;
  negativeCapitalInvested: number;
  positiveCapitalPercent: number | null;
  negativeCapitalPercent: number | null;
  positiveGain: number;
  negativeGain: number;
  positiveGainPercent: number | null;
  negativeGainPercent: number | null;
  averageHoldingYears: number | null;
  weightedHoldingYears: number | null;
  bestHoldingBucket: string;
  cagr: number | null;
}

export interface HoldingBucketSummary {
  label: string;
  tradeCount: number;
  capitalInvested: number;
  totalGain: number;
  returnPercent: number | null;
  positiveTradePercent: number | null;
}

export interface TradeCalculationOptions {
  includeFx: boolean;
  includeIncome: boolean;
  includeReinvestments: boolean;
  account: FinancialAccount | null;
  locale: string;
  periodAgo?: string | null;
}

export function includedTrades(trades: PortfolioTrade[], includeReinvestments: boolean): PortfolioTrade[] {
  const rollupTrades = trades.filter(trade => !trade.ignoredForRollup);
  if (includeReinvestments) {
    return rollupTrades;
  }
  return rollupTrades.filter(trade => !isReinvestmentTrade(trade));
}

export function getTradeDeltaAmount(trade: PortfolioTrade, options: TradeCalculationOptions): number | null {
  const combinedDelta = options.includeFx ? trade.deltaAmountWithFx : trade.deltaAmountWithoutFx;
  const marketDelta =
    combinedDelta !== null && combinedDelta !== undefined
      ? combinedDelta
      : options.includeFx
        ? (trade.realizedDeltaAmountWithFx ?? null)
        : (trade.realizedDeltaAmountWithoutFx ?? null);
  if (marketDelta === null || marketDelta === undefined) {
    return null;
  }
  return marketDelta + (options.includeIncome ? (getTradeDividendIncomeAmount(trade, options) ?? 0) : 0);
}

export function getTradeDeltaPercent(trade: PortfolioTrade, options: TradeCalculationOptions): number | null {
  const capital = getTradeCapitalInvested(trade, options);
  const delta = getTradeDeltaAmount(trade, options);
  if (capital === null || delta === null || capital === 0) {
    return null;
  }
  return (delta / capital) * 100;
}

export function getTradeDeltaCurrencyCode(trade: PortfolioTrade, options: TradeCalculationOptions): string {
  return options.includeFx ? (options.account?.currencyCode ?? 'AUD') : (trade.currencyCode ?? 'AUD');
}

export function isReinvestmentTrade(trade: PortfolioTrade): boolean {
  const type = trade.type?.toUpperCase();
  return type === 'REINVEST_DIVIDEND_COMBINED' || type === 'DIVDEND_REINVESTMENT' || type === 'REINVEST_DIVIDEND';
}

export function getTradeSummaryCurrencyCode(options: TradeCalculationOptions): string {
  return options.includeFx ? (options.account?.currencyCode ?? 'AUD') : 'AUD';
}

export function getTradeDividendIncomeAmount(trade: PortfolioTrade, options: TradeCalculationOptions): number | null {
  const dividends = getFilteredTradeDividends(trade, options);
  if (dividends.length > 0) {
    let hasValue = false;
    const total = dividends.reduce((sum, dividend) => {
      const amount = options.includeFx ? dividend.amountWithFx : dividend.amountWithoutFx;
      if (amount === null || amount === undefined) {
        return sum;
      }
      hasValue = true;
      return sum + amount;
    }, 0);
    return hasValue ? total : 0;
  }

  if ((trade.dividends?.length ?? 0) > 0 && !!normalizeTradePeriod(options.periodAgo)) {
    return 0;
  }

  return options.includeFx ? (trade.dividendIncomeAmountWithFx ?? null) : (trade.dividendIncomeAmountWithoutFx ?? null);
}

export function getTradeDividendIncomeCurrencyCode(trade: PortfolioTrade, options: TradeCalculationOptions): string {
  const dividends = getFilteredTradeDividends(trade, options);
  return options.includeFx
    ? (options.account?.currencyCode ?? dividends[0]?.baseCurrencyCode ?? trade.dividends?.[0]?.baseCurrencyCode ?? 'AUD')
    : (trade.currencyCode ?? 'AUD');
}

export function hasTradeDividends(trade: PortfolioTrade, options?: TradeCalculationOptions): boolean {
  if (!options) {
    return (trade.dividends?.length ?? 0) > 0;
  }
  return getFilteredTradeDividends(trade, options).length > 0;
}

export function getTradeCapitalInvested(trade: PortfolioTrade, options: TradeCalculationOptions): number | null {
  if (trade.buyPrice === null || trade.buyPrice === undefined || trade.quantity === null || trade.quantity === undefined) {
    return null;
  }
  const baseCapital = trade.buyPrice * trade.quantity;
  if (!options.includeFx) {
    return baseCapital;
  }
  return baseCapital * (trade.buyFxRate ?? 1);
}

export function getTradeExitPrice(trade: PortfolioTrade): number | null {
  return trade.sold ? (trade.sellPrice ?? trade.currentPrice ?? null) : (trade.currentPrice ?? null);
}

export function getTradeExitFxRate(trade: PortfolioTrade): number | null {
  return trade.sold ? (trade.sellFxRate ?? trade.currentFxRate ?? null) : (trade.currentFxRate ?? null);
}

export function getTradeStatus(trade: PortfolioTrade, locale: string): string {
  if (!trade.sold) {
    return 'Open';
  }
  return trade.sellDate ? `Sold ${new Date(trade.sellDate).toLocaleDateString(locale)}` : 'Sold';
}

export function getTradeHoldingYears(trade: PortfolioTrade): number | null {
  if (trade.holdingYears !== null && trade.holdingYears !== undefined) {
    return trade.holdingYears;
  }
  const date = new Date(trade.date);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  const exitDate = trade.sold && trade.sellDate ? new Date(trade.sellDate) : new Date();
  if (Number.isNaN(exitDate.getTime())) {
    return null;
  }
  return Math.max(0, (exitDate.getTime() - date.getTime()) / (365.25 * 24 * 60 * 60 * 1000));
}

export function formatTradeCurrency(value: number | null | undefined, currencyCode: string | null | undefined, locale: string): string {
  if (value === null || value === undefined) {
    return '—';
  }
  const resolvedCurrencyCode = currencyCode ?? 'AUD';
  return formatCurrency(value, locale, getCurrencySymbol(resolvedCurrencyCode, 'narrow'), resolvedCurrencyCode);
}

export function formatTradeSummaryCurrency(value: number | null | undefined, options: TradeCalculationOptions): string {
  return formatTradeCurrency(value, getTradeSummaryCurrencyCode(options), options.locale);
}

export function formatTradeNumber(value: number | null | undefined, locale: string, digits = '1.1-1'): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—';
  }
  return value.toLocaleString(locale, {
    minimumFractionDigits: Number(digits.split('.')[1]?.split('-')[0] ?? 1),
    maximumFractionDigits: Number(digits.split('-')[1] ?? 1),
  });
}

export function formatTradePercent(value: number | null | undefined, locale: string): string {
  if (value === null || value === undefined) {
    return '—';
  }
  return `${(value / 100).toLocaleString(locale, { style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function formatTradeFxRate(value: number | null | undefined, locale: string): string {
  if (value === null || value === undefined) {
    return '—';
  }
  return value.toLocaleString(locale, { minimumFractionDigits: 4, maximumFractionDigits: 4 });
}

export function getFilteredTradeDividends(trade: PortfolioTrade, options: TradeCalculationOptions): PortfolioTradeDividend[] {
  const dividends = trade.dividends ?? [];
  const period = normalizeTradePeriod(options.periodAgo);
  const periodStart = getTradePeriodStart(period);
  if (!periodStart) {
    return dividends;
  }

  return dividends.filter(dividend => {
    const dividendDate = new Date(dividend.date);
    return !Number.isNaN(dividendDate.getTime()) && dividendDate >= periodStart;
  });
}

export function getTradePeriodStart(periodAgo: string | null | undefined, referenceDate = new Date()): Date | null {
  const period = normalizeTradePeriod(periodAgo);
  if (!period) {
    return null;
  }

  const start = new Date(referenceDate);
  start.setHours(0, 0, 0, 0);

  switch (period) {
    case '1D':
      start.setDate(start.getDate() - 1);
      return start;
    case '1W':
      start.setDate(start.getDate() - 7);
      return start;
    case '1M':
      start.setMonth(start.getMonth() - 1);
      return start;
    case '1Q':
      start.setMonth(start.getMonth() - 3);
      return start;
    case '1Y':
      start.setFullYear(start.getFullYear() - 1);
      return start;
    case '3Y':
      start.setFullYear(start.getFullYear() - 3);
      return start;
    case '5Y':
      start.setFullYear(start.getFullYear() - 5);
      return start;
    case '10Y':
      start.setFullYear(start.getFullYear() - 10);
      return start;
    default:
      return null;
  }
}

function normalizeTradePeriod(periodAgo: string | null | undefined): string {
  const normalized = (periodAgo ?? '').trim().toUpperCase();
  if (!normalized || normalized === 'ALL') {
    return '';
  }
  return normalized;
}

export function getTradeRowClass(trade: PortfolioTrade, options: TradeCalculationOptions): string {
  if (trade.ignoredForRollup) {
    return 'trade-row--ignored';
  }
  const delta = getTradeDeltaAmount(trade, options);
  if (delta == null) {
    return '';
  }
  if (delta > 0) {
    return 'trade-row--positive';
  }
  if (delta < 0) {
    return 'trade-row--negative';
  }
  return '';
}

export function safePercent(numerator: number, denominator: number): number | null {
  if (!denominator) {
    return null;
  }
  return (numerator / denominator) * 100;
}

export function cagr(startValue: number, endValue: number, years: number): number | null {
  if (startValue <= 0 || endValue <= 0 || years <= 0) {
    return null;
  }
  return (Math.pow(endValue / startValue, 1 / years) - 1) * 100;
}

export function holdingBucketLabel(holdingYears: number): string {
  if (holdingYears < 1) {
    return '<1 year';
  }
  if (holdingYears < 2) {
    return '1-2 years';
  }
  if (holdingYears < 3) {
    return '2-3 years';
  }
  if (holdingYears < 5) {
    return '3-5 years';
  }
  return '5+ years';
}

export function bestHoldingBucketLabel(buckets: HoldingBucketSummary[]): string {
  const ranked = buckets.filter(bucket => bucket.tradeCount > 0 && bucket.returnPercent !== null);
  if (ranked.length === 0) {
    return '—';
  }
  ranked.sort((left, right) => (right.returnPercent ?? Number.NEGATIVE_INFINITY) - (left.returnPercent ?? Number.NEGATIVE_INFINITY));
  return ranked[0].label;
}

export function emptyTradeSummaryYear(year: number): TradeSummaryYear {
  return {
    year,
    capitalInvested: 0,
    cumulativeCapitalInvested: 0,
    tradeCount: 0,
    positiveTradeCount: 0,
    negativeTradeCount: 0,
    positiveTradePercent: null,
    negativeTradePercent: null,
    positiveCapitalInvested: 0,
    negativeCapitalInvested: 0,
    positiveCapitalPercent: null,
    negativeCapitalPercent: null,
    positiveGain: 0,
    positiveGainPercent: null,
    positiveGainCumulative: 0,
    negativeGain: 0,
    negativeGainCumulative: 0,
    negativeGainPercent: null,
    totalGain: 0,
    totalGainPercent: null,
    averageHoldingYears: null,
    cagr: null,
  };
}
