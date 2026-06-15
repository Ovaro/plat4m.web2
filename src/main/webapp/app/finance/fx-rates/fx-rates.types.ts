export interface FinanceFXRate {
  id: string;
  date: string;
  fromIsoCode: string;
  toIsoCode: string;
  rate: number;
}

export interface FinanceFXHistoryPoint {
  date: string;
  rate: number;
}

export interface FinanceFXRateUpdate {
  date: string;
  fromIsoCode: string;
  toIsoCode: string;
  rate: number;
}

export interface FinanceFXImportRequest {
  baseCurrency: string;
  quoteCurrencies: string[];
  date: string | null;
}

export interface FinanceFXImportResult {
  date: string;
  baseCurrency: string;
  updated: number;
}
