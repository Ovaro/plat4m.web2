export interface DemoDataAccountSpec {
  accountType: 'bank' | 'credit' | 'loan' | 'asset' | 'investment';
  include: boolean;
  accountCount: number;
  years: number;
  density: 'low' | 'medium' | 'high';
  startingValue: number;
  targetValue?: number | null;
  stockCount?: number | null;
  tradingProfile?: 'buy_hold' | 'core_satellite' | 'limited_trading' | 'active_trading' | 'day_trader' | null;
  hints?: string | null;
}

export interface DemoDataRequest {
  persona: string;
  currencyCode: string;
  hints: string;
  domesticStocks: boolean;
  internationalStocks: boolean;
  realEstateInvestor: boolean;
  homeOwner: boolean;
  renter: boolean;
  privateSchool: boolean;
  noInvestments: boolean;
  useAiHints: boolean;
  spendingRatePercent: number;
  accounts: DemoDataAccountSpec[];
}

export interface DemoDataOptions {
  personas: string[];
  variants: string[];
  accountTypes: string[];
  yearOptions: number[];
}

export interface DemoDataResult {
  generatedBatchId: string;
  generatedLabel: string;
  accountCount: number;
  transactionCount: number;
  payeeCount: number;
  securityCount: number;
  priceCount: number;
  lotCount: number;
  accountNames: string[];
  notes: string[];
  preview: boolean;
}

export interface DemoDataBatch {
  generatedBatchId: string;
  generatedLabel: string;
  createdAt: string;
  accountCount: number;
  transactionCount: number;
  payeeCount: number;
  securityCount: number;
  priceCount: number;
  lotCount: number;
  accountNames: string[];
}

export interface DemoDataDeleteResult {
  generatedBatchId: string;
  generatedLabel: string;
  accountCount: number;
  transactionCount: number;
  payeeCount: number;
  securityCount: number;
  priceCount: number;
  lotCount: number;
  institutionCount: number;
  sourceLinkCount: number;
}
