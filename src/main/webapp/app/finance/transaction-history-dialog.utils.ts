import { ApexAxisChartSeries, ApexChart, ApexDataLabels, ApexMarkers, ApexStroke, ApexTooltip, ApexXAxis, ApexYAxis } from 'ng-apexcharts';

import { FinancialTransaction } from './finance.model';

export type TransactionHistoryRange = 'month' | 'quarter' | 'halfYear' | 'year' | 'twoYears' | 'fiveYears' | 'all';

export interface TransactionHistoryRangeOption {
  label: string;
  value: TransactionHistoryRange;
}

export interface TransactionPoint {
  x: number;
  y: number;
}

export interface TrendlineSeriesResult {
  rSquared: number | null;
  series: ApexAxisChartSeries[number];
}

export type TransactionAmountChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  dataLabels: ApexDataLabels;
  markers: ApexMarkers;
  stroke: ApexStroke;
  tooltip: ApexTooltip;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
};

export const TRANSACTION_HISTORY_RANGE_OPTIONS: TransactionHistoryRangeOption[] = [
  { label: 'Last month', value: 'month' },
  { label: 'Last quarter', value: 'quarter' },
  { label: 'Last 6 months', value: 'halfYear' },
  { label: 'Last year', value: 'year' },
  { label: 'Last 2 years', value: 'twoYears' },
  { label: 'Last 5 years', value: 'fiveYears' },
  { label: 'All', value: 'all' },
];

export function createTransactionChartOptions(formatter: (value: number | null | undefined) => string): TransactionAmountChartOptions {
  return {
    series: [],
    chart: {
      type: 'area',
      height: 280,
      toolbar: { show: false },
      zoom: { enabled: false },
      animations: { enabled: false },
    },
    dataLabels: { enabled: false },
    markers: { size: 3, hover: { sizeOffset: 2 } },
    stroke: { curve: 'smooth', width: 2 },
    tooltip: {
      x: { format: 'dd MMM yyyy' },
      y: {
        formatter: value => formatter(value),
      },
    },
    xaxis: { type: 'datetime' },
    yaxis: {
      labels: {
        formatter: value => formatter(value),
      },
    },
  };
}

export function filterTransactionsByRange(
  transactions: FinancialTransaction[],
  range: TransactionHistoryRange,
  now: Date = new Date(),
): FinancialTransaction[] {
  const startDate = getRangeStartDate(range, now);
  if (!startDate) {
    return transactions;
  }

  return transactions.filter(transaction => toTransactionTime(transaction.date) >= startDate.getTime());
}

export function buildTransactionPoints(transactions: FinancialTransaction[]): TransactionPoint[] {
  return [...transactions]
    .sort((left, right) => toTransactionTime(left.date) - toTransactionTime(right.date))
    .map(transaction => ({
      x: toTransactionTime(transaction.date),
      y: Math.abs(Number(transaction.amount ?? 0)),
    }));
}

export function createTrendlineSeries(points: TransactionPoint[]): TrendlineSeriesResult | null {
  if (points.length < 2) {
    return null;
  }

  const count = points.length;
  const sumX = points.reduce((sum, point) => sum + point.x, 0);
  const sumY = points.reduce((sum, point) => sum + point.y, 0);
  const sumXY = points.reduce((sum, point) => sum + point.x * point.y, 0);
  const sumXX = points.reduce((sum, point) => sum + point.x * point.x, 0);
  const denominator = count * sumXX - sumX * sumX;

  if (denominator === 0) {
    return null;
  }

  const slope = (count * sumXY - sumX * sumY) / denominator;
  const intercept = (sumY - slope * sumX) / count;
  const trendlinePoints = points.map(point => ({
    x: point.x,
    y: slope * point.x + intercept,
  }));

  const meanY = sumY / count;
  const ssTot = points.reduce((sum, point) => sum + (point.y - meanY) ** 2, 0);
  const ssRes = points.reduce((sum, point, index) => sum + (point.y - trendlinePoints[index].y) ** 2, 0);
  const rSquared = ssTot === 0 ? 1 : Math.max(0, 1 - ssRes / ssTot);

  return {
    rSquared,
    series: {
      name: formatTrendlineLabel(rSquared),
      data: trendlinePoints,
    },
  };
}

export function formatTrendlineLabel(rSquared: number | null): string {
  if (rSquared == null) {
    return 'Trendline';
  }

  return `Trendline (${describeRSquared(rSquared)}, R² ${rSquared.toFixed(2)})`;
}

export function formatCurrencyAmount(amount: number, locale: string, currencyCode: string): string {
  const formatted = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currencyCode,
    currencyDisplay: 'narrowSymbol',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Math.abs(amount));

  return amount < 0 ? `(${formatted})` : formatted;
}

export function inferTransactionCurrencyCode(
  transactions: FinancialTransaction[],
  getCurrencyCode: (accountId: string | null | undefined) => string,
  fallbackCurrencyCode: string,
): string {
  const currencies = new Set(
    transactions.map(transaction => getCurrencyCode(transaction.accountId)).filter(currencyCode => Boolean(currencyCode)),
  );

  return currencies.size === 1 ? [...currencies][0] : fallbackCurrencyCode;
}

export function toTransactionTime(date: string): number {
  return new Date(`${date}T00:00:00`).getTime();
}

function getRangeStartDate(range: TransactionHistoryRange, now: Date): Date | null {
  if (range === 'all') {
    return null;
  }

  const monthsByRange: Record<Exclude<TransactionHistoryRange, 'all'>, number> = {
    month: 1,
    quarter: 3,
    halfYear: 6,
    year: 12,
    twoYears: 24,
    fiveYears: 60,
  };

  const startDate = new Date(now);
  startDate.setHours(0, 0, 0, 0);
  startDate.setMonth(startDate.getMonth() - monthsByRange[range]);
  return startDate;
}

function describeRSquared(rSquared: number): string {
  if (rSquared >= 0.9) {
    return 'very strong fit';
  }
  if (rSquared >= 0.75) {
    return 'strong fit';
  }
  if (rSquared >= 0.5) {
    return 'moderate fit';
  }
  if (rSquared >= 0.25) {
    return 'weak fit';
  }
  return 'very weak fit';
}
