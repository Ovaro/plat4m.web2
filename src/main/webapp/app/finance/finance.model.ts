export interface Periods {
  name: string;
  value: string;
}

export interface PanelSpec {
  cols: number;
  rows: number;
  x: number;
  y: number;
  panelType: string;
  section: string;
  dataTypeSpecs: DataTypeSpec[];
  //  title: string,
  //  dataType: string[],
  //  compositeDataTypeOperation: string | null,
  //  negativeAsPositive: boolean | null,
  //  detailedPanelSpec?: DetailPanelSpec,
  //  expandGroups: boolean | null
}

export interface DataTypeSpec {
  title: string;
  dataType: string[];
  compositeDataTypeOperation: string | null;
  negativeAsPositive: boolean | null;
  detailedPanelSpec?: DetailPanelSpec;
  expandGroups: boolean | null;
}

export interface DetailPanelSpec {
  type: string;
  chartType?: string;
  combineSeries: boolean | null;
}

export class FinancialAccount {
  constructor(
    public id: string,
    public name: string,
    public type: number,
    public accountType: string,
    // public lastName: string | null,
    public currencyCode: string,
    public balance: number,
    public balanceWarning: string,
    public fxRateToLocal: number | null,
    public fxDateTime: string | null,
    public relatedToAccountId: string | null,
    public institution: Institution | null,
    public startingBalance: number,
  ) {}
}

export class Currency {
  constructor(
    public id: string,
    public isoCode: string,
    public name: string,
  ) {}
}

export class Institution {
  constructor(
    public id: string,
    public name: string,
    public icon: string,
  ) {}
}

export class FinanceSecurityHolding {
  constructor(
    public id: string,
    public name: string,
    public symbol: string,
    public userSymbol: string,
    public currencyCode: string,
    public type: number,
    public typeName: string,
    public sector: string,
    public industry: string,
    public exchangeName: string,
    public linked: string,
    public masterGuid: string,
    public comment: string,
    public quantity: number,
    public price: number,
    public value: number,
    public valueInBaseCurrency: number,
    public fxRateToLocal: number,
    public fxDateTime: string,
    public accountId: string,
    public accountName: string,
    public priceDateTime: string,
  ) {}
}

export class InvestmentTransaction {
  constructor(
    public date: string,
    public price: number,
    public quantity: number,
    public amount: number,
    public type: string,
    public currencyCode: string,
    public transaction: FinancialTransaction,
  ) {}
}

// export class InvestmentDetails {
//   constructor(
//     public userSecurityId: string,
//     public priceDateTime: string,
//     public price: number,
//     public quantity: number,
//     public totalCapitalInvested: number,
//     public totalCapitalGain: number,
//     public totalCurrencyGain: number,
//     public totalIncome: number,
//     public totalReturn: number,
//     public totalIncomeCAGR: number,
//     public totalIncomePC: number,
//     public totalReturnCAGR: number,
//     public totalReturnPC: number,
//     public totalCapitalGainCAGR: number,
//     public totalCapitalGainPC: number,
//     public totalCurrencyGainCAGR: number,
//     public totalCurrencyGainPC: number,
//     public totalFees: number,
//     public currencyCode: string,
//     public transactions: InvestmentTransaction[],
//     public fxRateToLocal: number | null,
//     public fxDateTime: string | null,
//     public modDietzPc: number,
//     public modDietzPcpa: number,
//     /** Transient/Augmented Data after local processing */
//     public comparisonDateTime: string,
//     public comparisonPrice: number,
//     public comparisonPriceDateTime: string,
//     public comparisonQuantity: number,
//     public comparisonTotalCapitalInvested: number,
//     public comparisonTotalCapitalGain: number,
//     public comparisonTotalCurrencyGain: number,
//     public comparisonTotalIncome: number,
//     public comparisonTotalReturn: number
//   ){}
// };

export class InvestmentPortfolioDetails {
  constructor(
    public date: string,
    public summaries: FinanceInvestmentSnapshotDetails[],
    // FinanceInvestmentSnapshotDetails below
    public priceDateTime: string,
    public price: number,
    public quantity: number,
    public estimatedPrice: boolean | null,
    public totalCapitalInvested: number,
    public totalCapitalGain: number,
    public totalCurrencyGain: number,
    public totalIncome: number,
    public totalReturn: number,
    public totalIncomeCAGR: number,
    public totalIncomePC: number,
    public totalReturnCAGR: number,
    public totalReturnPC: number,
    public totalCapitalGainCAGR: number,
    public totalCapitalGainPC: number,
    public totalCurrencyGainCAGR: number,
    public totalCurrencyGainPC: number,
    public totalFees: number,
    public currencyIsoCode: string,
    public userSecurityId?: string | null,
  ) {}
}

export class FinancialTransaction {
  constructor(
    public id: string,
    public date: string,
    public name: string,
    public type: number,
    public payeeName: string,
    public payeeId: string,
    public memo: string | null,
    public amount: number,
    public runningBalance: number,
    public categoryId: string,
    public categoryName: string,
    public parentCategoryId: string,
    public parentCategoryName: string,
    public splitParent: boolean,
    public splitChild: boolean,
    public transferredAccountId: string,
    public cleared: boolean,
    public voided: boolean,
    public number: number,
    // DISPLAY FIELDS
    public payment: number,
    public deposit: number,
    public displayCategory: string,
  ) {}
}

export class Source {
  constructor(
    public id: string,
    public name: string,
    public typeId: number,
    public typeName: string,
    public lastSyncDateTime: string,
    public lastUpdateDateTime: string,
  ) {}
}

export class FinanceSnapshot {
  constructor(
    public id: string,
    public date: string,
    public value: number,
    public type: string,
    public fxToLocal?: number | null,
    public fxDate?: string | null,
    public currencyIsoCode?: string | null,
  ) {}
}

export class FinanceInvestmentSnapshotDetails {
  constructor(
    public priceDateTime: string,
    public price: number,
    public quantity: number,
    public estimatedPrice: boolean | null,
    public totalCapitalInvested: number,
    public totalCapitalGain: number,
    public totalCurrencyGain: number,
    public totalIncome: number,
    public totalReturn: number,
    public totalIncomeCAGR: number,
    public totalIncomePC: number,
    public totalReturnCAGR: number,
    public totalReturnPC: number,
    public totalCapitalGainCAGR: number,
    public totalCapitalGainPC: number,
    public totalCurrencyGainCAGR: number,
    public totalCurrencyGainPC: number,
    public totalFees: number,
    public currencyIsoCode: string,
    public fxToLocal?: number | null,
    public fxDate?: string | null,
    public userSecurityId?: string | null,
  ) {}
}

export class FinanceResource {
  constructor(
    public id: string,
    public name: string,
    public symbol: string,
    public currencyCode: string,
    public type: string,
  ) {}
}

export class FinanceNestedResource extends FinanceResource {
  constructor(
    public id: string,
    public name: string,
    public symbol: string,
    public currencyCode: string,
    public type: string,
    public children: [FinanceNestedResource],
  ) {
    super(id, name, symbol, currencyCode, type);
  }
}

export class FinanceResourceSnapshots extends FinanceResource {
  constructor(
    public id: string,
    public name: string,
    public symbol: string,
    public currencyCode: string,
    public type: string,
    public snapshots: FinanceSnapshot[],
    public annotations: Annotation[],
  ) {
    super(id, name, symbol, currencyCode, type);
  }
}

export class SnapshotWithDelta {
  constructor(
    public id: string,
    public date: string,
    public value: number,
    public type: string,
    public comparisonValue: number,
    public naValue: boolean = false,
    public fxToLocal: number | null,
    public fxDate: string | null,
    public currencyIsoCode: string | null,
  ) {}
}

export class FinanceIndicator extends FinanceResource {
  constructor(
    public id: string,
    public name: string,
    public symbol: string,
    public currencyCode: string,
    public type: string,
    public snapshot: SnapshotWithDelta,
  ) {
    super(id, name, symbol, currencyCode, type);
  }
}

export class FinanceIndicators {
  constructor(public indicators: FinanceIndicator[]) {}
}

export class Annotation {
  constructor(
    public date: string,
    public annotation: string,
    public type: string,
    public quantity: number,
    public price: number,
    public totalValue: number,
  ) {}
}

// export class ValueSnapshotsPerId {
//   constructor(
//     public id: string,
//     public snapshots: ValueSnapshot[]
//    ) {}
// }

// export class AnnotationsPerId {
//   constructor(
//     public id: string,
//     public annotations: Annotation[]
//    ) {}
// }

export class PortfolioValueHistoryItem {
  constructor(
    public security: FinanceSecurityHolding,
    public valueOverTimePerSecurity: FinanceSnapshot[],
    public annotationsPerSecurity: Annotation[],
  ) {}
}

// export class PortfolioValueHistory {
//   constructor(
//     public portfolioValueHistoryItems: PortfolioValueHistoryItem[]
//    ) {}
// }

export class Panel {
  constructor(
    public type: string,
    public dataType: string,
  ) {}
}

// [
//   {
//     "id": "94792777-5638-488e-ab18-9ee5b61ae87a",
//     "userGuid": "715bbe5e-2628-4da5-8737-d0f7e10a8148",
//     "number": 2251,
//     "accountId": "e7757a5f-6fef-4759-9fab-0f343a30a47d",
//     "amount": 108.37,
//     "statusFlag": null,
//     "date": "2004-01-30",
//     "recurring": false,
//     "categoryId": 134,
//     "sourcePayeeId": 47,
//     "payeeId": "f2f888ed-d2ac-462a-94c3-21be9ab9565c",
//     "transferredAccountId": null,
//     "securityId": null,
//     "investmentActivityType": null,
//     "quantity": null,
//     "price": null,
//     "transfer": false,
//     "transferTo": false,
//     "investment": false,
//     "splitParent": false,
//     "splitChild": false,
//     "voided": false,
//     "statementId": null,
//     "cleared": false,
//     "reconciled": false,
//     "memo": null,
//     "serialDateTime": "2016-07-20T12:19:03+10:00",
//     "payeeName": "CitiBank",
//     "masterGuid": "4234BF15-7846-4D50-BCA8-02A9F1F0FC1F"
//   },

//   "id": "587e1085-6f9e-4ee2-a6e6-10cef1177756",
//     "userGuid": "715bbe5e-2628-4da5-8737-d0f7e10a8148",
//     "name": "ASX Share Investments",
//     "type": 5,
//     "currency": {
//       "id": "3a5ff3b7-c4b7-4406-9827-ab08f69d2e76",
//       "name": "Australian dollar",
//       "isoCode": "AUD",
//       "masterGuid": "225903A4-8DD6-4C0C-8D97-E52593DED354"
//     },
//     "startingBalance": 0.00,
//     "relatedToAccountId": null,
//     "relatedToAccount": null,
//     "closed": false,
//     "retirement": false,
//     "investmentSubType": null,
//     "amountLimit": null,
//     "credit": false,
//     "is401k403b": false,
//     "dateOpened": null,
//     "dateClosed": null,
//     "interestRate": null,
//     "dateInterestRateChanged": null,
//     "serialDateTime": null,
//     "maxPayments": null,
// "institution": {
//   "id": "ccce9946-677e-4b98-aeff-ef1f7eddd80a",
//   "name": "American Express",
//   "comment": null,
//   "icon": null,
//   "serialDateTime": null
// },
//     "balance": 5838.05
