import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  inject,
  NgZone,
  OnInit,
  ViewChild,
  ViewEncapsulation,
} from '@angular/core';
import { CommonModule, CurrencyPipe, formatCurrency, NgForOf, registerLocaleData } from '@angular/common';
import { GridsterComponent, GridsterItemComponent, GridsterModule } from 'angular-gridster2';
import { GridsterConfig, GridsterItem } from 'angular-gridster2';
import locale from '@angular/common/locales/en-AU';
import {
  Annotation,
  FinanceIndicator,
  FinanceIndicators,
  FinanceNestedResource,
  FinanceResource,
  FinanceResourceSnapshots,
  FinanceSecurityHolding,
  FinanceSnapshot,
  PanelSpec,
  Periods,
} from '../finance.model';
import { DashboardService } from './dashboard.service';

import { CookieService } from 'ngx-cookie';
import {
  ApexAnnotations,
  ApexAxisChartSeries,
  ApexChart,
  ApexOptions,
  ApexTitleSubtitle,
  ApexTooltip,
  ApexFill,
  ApexXAxis,
  ApexYAxis,
  ChartComponent,
  ChartType,
  NgApexchartsModule,
} from 'ng-apexcharts';
import SharedModule from 'app/shared/shared.module';
import { SelectButtonModule } from 'primeng/selectbutton';
import { FormsModule } from '@angular/forms';
import { SplitterModule } from 'primeng/splitter';
import { FinanceModule } from 'finance/finance.module';
import { ParentDynamicComponent } from './parentDynamic.component';

export type HistoryChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  // title: ApexTitleSubtitle;
  stacked: boolean;
  options: ApexOptions;
  // tooltip: ApexTooltip;
  // annotations: ApexAnnotations;
};

// declare type XYData = {
//   x: any;
//   y: any;
//   fill?: ApexFill;
//   fillColor?: string;
//   strokeColor?: string;
//   meta?: any;
//   goals?: any;
//   barHeightOffset?: number;
//   columnWidthOffset?: number;
// };

@Component({
  selector: 'jhi-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [SharedModule, SelectButtonModule, FormsModule, GridsterModule, NgApexchartsModule, SplitterModule, ParentDynamicComponent],
  // standalone: true,
  // imports: [
  //   NgForOf,
  //   GridsterComponent,
  //   GridsterItemComponent,
  //   ParentDynamicComponent
  // ]
})
export class DashboardComponent {
  //  implements OnInit
  static PERIOD_DEFAULT_COOKIE_ID = 'period-default';
  static RESOURCE_GROUP_PREFIX = 'rg/';
  static RG_ACCOUNTS_BY_TYPE = `{RESOURCE_GROUP_PREFIX}AccountsByType`;
  static RG_ACCOUNTS_BY_CURRENCY = `{RESOURCE_GROUP_PREFIX}AccountsByCurrency`;
  static RG_ACCOUNT_TYPE_PREFIX = `{RESOURCE_GROUP_PREFIX}AccountType-`;
  static RG_ACCOUNT_CURRENCY_PREFIX = `{RESOURCE_GROUP_PREFIX}AccountCurrency-`;

  static CHILDREN_OF_PREFIX = 'childrenOf:';
  static JSON = JSON;

  options: GridsterConfig = {};
  dashboard: Array<GridsterItem>;
  liabilityPanel: GridsterItem;
  isLoading = false;
  isLoadingPortfolioIndicators = false;
  isLoadingAccountSnapshots = false;
  isLoadingPortfolioSnapshots = false;

  panelSpecs: Array<PanelSpec>;

  @ViewChild('gridster', { static: false }) gridster!: GridsterComponent;

  selectedItem: GridsterItem | null = null;
  refreshItem: GridsterItem | null = null;

  resizeEvent: EventEmitter<GridsterItem> = new EventEmitter<GridsterItem>();

  periods: Periods[];
  selectedPeriod: Periods;

  allResources: FinanceNestedResource[];

  accountSnapshots: FinanceResourceSnapshots[];
  portfolioSnapshots: FinanceResourceSnapshots[];

  financeIndicators: FinanceIndicator[];

  @ViewChild('historyChart', { static: false }) historyChart!: ChartComponent;
  public historyChartOptions: Partial<HistoryChartOptions>;

  //  eventsSubject: Subject<void> = new Subject<void>();

  itemChange(item: any, itemComponent: any) {
    console.info('itemChanged', item, itemComponent);

    //this.resizeEvent.emit(item);
  }

  itemResize(item: any, itemComponent: any) {
    // console.info('itemResized', item, itemComponent);
    if (this.resizeEvent) {
      this.resizeEvent.emit(item);
      // console.info('itemResized- emitted');
    }
  }

  private dashboardService = inject(DashboardService);
  //private currencyPipe  = inject(CurrencyPipe);
  private cookieService = inject(CookieService);

  constructor(
    private changeDetectorRef: ChangeDetectorRef,
    private zone: NgZone,
  ) {
    this.options = {
      itemChangeCallback: this.itemChange,
      // itemResizeCallback: item => {
      //   // update DB with new size
      //   // send the update to widgets
      //   this.resizeEvent.emit(item);
      // },
      itemResizeCallback: this.itemResize,
      displayGrid: 'none',
      draggable: {
        enabled: true,
      },
      gridType: 'fixed',
      fixedRowHeight: 152,
      fixedColWidth: 132,
      keepFixedHeightInMobile: true,
      mobileBreakpoint: 480,
    };

    this.liabilityPanel = { cols: 1, rows: 1, y: 0, x: 4 };
    this.dashboard = [];
    this.panelSpecs = [];

    this.periods = [
      { name: '1D', value: '1D' },
      { name: '1W', value: '1W' },
      { name: '1M', value: '1M' },
      { name: '1Q', value: '1Q' },
      { name: '1Y', value: '1Y' },
      { name: '3Y', value: '3Y' },
      { name: '5Y', value: '5Y' },
      { name: '10Y', value: '10Y' },
      { name: 'ALL', value: '' },
    ];
    this.selectedPeriod = this.periods[this.periods.length - 1];
    this.accountSnapshots = [];
    this.portfolioSnapshots = [];
    this.financeIndicators = [];
    this.allResources = [];
    this.historyChartOptions = {};
  }

  ngOnInit(): void {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const ptr = this;

    this.liabilityPanel = {
      cols: 1,
      rows: 1,
      y: 0,
      x: 4,
      type: 'standard',
      selected: false,
      header: 'Liabilities',
      title: 'Home Loan',
      dataType: 'sumAccountType-loan',
      negativeAsPositive: true,
    }; // value: 0, comparisonValue: 0, negativeAsPositive: true, periodAgo: '3Y', dataClass: 'loan'
    //let xCol =2;
    this.panelSpecs = [
      {
        cols: 2,
        rows: 1,
        y: 0,
        x: 0,
        panelType: 'wide',
        section: 'Liabilities',
        dataTypeSpecs: [
          {
            title: 'All Liabilities',
            dataType: ['sumAccountType-loan', 'sumAccountType-credit'],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: null,
          },
          {
            title: 'Mortgage',
            dataType: ['sumAccountType-loan'],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: null,
          },
          {
            title: 'Mortgage (Real)',
            dataType: [
              '1c51bad5-13af-41ef-a64b-2a41e4f9509b',
              '3ceec315-9c2e-40c0-978b-bda1114def81',
              '5d2ed2e7-4f86-489c-aac3-3e43c87e729a',
              '753d5c1c-a4d8-4777-88bb-02f553d549c8',
              'b027589e-2ce4-4e71-a5a0-46f4b1d1a71e',
              'b2ad983b-656f-4067-8d7f-51e51ddeae76',
              'c032f51d-5e0c-417b-8929-ef31a9a76006',
              'f4e4a308-58b5-4b09-960e-b7f7ab2a44aa',
              '90a73fc4-755e-400a-be78-a6e4ab0f7736',
            ],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: 'sum',
            detailedPanelSpec: { type: 'chart', chartType: 'bar', combineSeries: false },
          },
          {
            title: 'Credit',
            dataType: ['sumAccountType-credit'],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: null,
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 0,
        x: 2,
        panelType: 'standard',
        section: 'Banking & Cash',
        dataTypeSpecs: [
          {
            title: 'All Banks',
            dataType: ['sumAccountType-bank', 'sumAccountType-cash'],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 0,
        x: 3,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'HSBC',
            dataType: [
              '1c51bad5-13af-41ef-a64b-2a41e4f9509b',
              '3ceec315-9c2e-40c0-978b-bda1114def81',
              '5d2ed2e7-4f86-489c-aac3-3e43c87e729a',
              '753d5c1c-a4d8-4777-88bb-02f553d549c8',
              'b027589e-2ce4-4e71-a5a0-46f4b1d1a71e',
              'b2ad983b-656f-4067-8d7f-51e51ddeae76',
              'c032f51d-5e0c-417b-8929-ef31a9a76006',
              'f4e4a308-58b5-4b09-960e-b7f7ab2a44aa',
            ],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
            detailedPanelSpec: { type: 'chart', chartType: 'bar', combineSeries: false },
          },
        ],
      },
      //{ cols: 1, rows: 1, y: 0, x: 7, panelType: 'standard', section: '', title: "Banks", dataType: ['sumAccountType-bank'], expandGroups: false, negativeAsPositive: false, compositeDataTypeOperation: "sum" },
      //{ cols: 1, rows: 1, y: 0, x: 8, panelType: 'standard', section: '', title: "Cash", dataType: ['sumAccountType-cash'], expandGroups: false, negativeAsPositive: false, compositeDataTypeOperation: "sum" },

      // OLD { cols: 1, rows: 1, y: 0, x: 11, panelType: 'standard', section: 'Investments', title: "Investments", dataType: ['sumAccountType-investment'], expandGroups: false, negativeAsPositive: false, compositeDataTypeOperation: "sum" },

      {
        cols: 2,
        rows: 1,
        y: 0,
        x: 4,
        panelType: 'wide',
        section: 'Investments',
        dataTypeSpecs: [
          {
            title: 'Investments',
            dataType: [`rg/AccountType-investment`],
            expandGroups: true,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
          {
            title: 'Core Portfolio',
            dataType: [
              '1874b1a4-6374-429b-81ce-447536e49355',
              '3e4dde99-d040-4dec-8d19-a387e567e077',
              'e39952d9-23d9-4145-9c06-ee6c02ae2978',
              'f4324207-f85d-4af9-9bf1-80f5ec3695ae',
            ],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
            detailedPanelSpec: { type: 'chart', chartType: 'area', combineSeries: false },
          },
          {
            title: 'Australian Shares',
            dataType: [`childrenOf:cad40a10-761a-400c-9bf3-6a2c4010971d`],
            expandGroups: true,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
          {
            title: 'US Shares',
            dataType: [`childrenOf:80bdc21f-e303-43ef-b803-a9bcbbbade14`, 'childrenOf:8f0bfad9-7e18-4c69-a693-d15d13ee0100'],
            expandGroups: true,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
          {
            title: 'Investment Cash',
            dataType: [
              'b2ad983b-656f-4067-8d7f-51e51ddeae76',
              'efbb288d-0409-4176-875c-b1ec1c77f74b',
              '6f47e743-3602-4df8-9b9e-fcdd64c7ebb8',
              '0f7b11e2-65d5-416e-ada9-c0a97066c51a',
            ],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
          },
        ],
      },
      {
        cols: 2,
        rows: 1,
        y: 0,
        x: 6,
        panelType: 'wide',
        section: 'Superannuation',
        dataTypeSpecs: [
          {
            title: 'All Super',
            dataType: ['childrenOf:0c312f79-47ec-4516-89f1-138d07bd7c9a', '881fd4a8-5a7e-410a-a7e4-c5c0a774fb61'],
            expandGroups: true,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
          {
            title: "James's Super",
            dataType: [`881fd4a8-5a7e-410a-a7e4-c5c0a774fb61`],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
          {
            title: "Mary's Super",
            dataType: [`0c312f79-47ec-4516-89f1-138d07bd7c9a`],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
        ],
      },
      {
        cols: 2,
        rows: 1,
        y: 0,
        x: 8,
        panelType: 'wide',
        section: 'Assets',
        dataTypeSpecs: [
          {
            title: 'All Assets',
            dataType: ['sumAccountType-asset', '2d34d828-d271-484e-bb7a-c1c64d134d8c', '370566c3-815e-488a-b4a4-00f2eb20c212'],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
          },
          {
            title: 'Home',
            dataType: ['d424c612-cf0d-413a-b491-119499b90266'],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
          },
          {
            title: 'Car(s)',
            dataType: ['f211bf6e-8aad-4c4a-a459-d92defd92e7b', 'b78fc59c-43b9-454f-ac91-cc6f88dc415e'],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
          },
          //{title: "Inventory", dataType: [], expandGroups: false, negativeAsPositive: false, compositeDataTypeOperation: "sum" }
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 0,
        x: 10,
        panelType: 'standard',
        section: 'Netwealth',
        dataTypeSpecs: [
          {
            title: 'Netwealth',
            dataType: ['rg/AccountsByType'],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
        ],
      },

      //{ cols: 1, rows: 1, y: 0, x: 12, panelType: 'standard', section: '', title: "US Shares", dataType: ['d0ab4146-ed14-44b8-9ae0-63b6c18df456', '275d5fd5-2fcf-40e6-a0b1-62357a60684a', 'e6a70d3b-cc5d-48e1-8ada-c334a24bf979'], expandGroups: false, negativeAsPositive: false, compositeDataTypeOperation: "sum" },
      //{ cols: 1, rows: 1, y: 0, x: 8, panelType: 'standard', section: '', dataTypeSpecs: [{title: "Investment Cash", dataType: ['41d00cea-f95a-4212-ab05-905edc3389ac', '75008e6e-a5da-4173-90ea-33c68b589f3e', 'b3fd4fec-b6fd-4be7-989e-24fbcf9bc6f6', '1f65e4ad-21e7-415a-8fac-0b41ca152df4'], expandGroups: false, negativeAsPositive: false, compositeDataTypeOperation: "sum" }]},

      //{ cols: 1, rows: 1, y: 0, x: 14, panelType: 'standard', section: 'Investments', title: "Australian Investments", dataType: ['22b07428-3dcc-48fd-bee2-e66841e84c49'], negativeAsPositive: false, compositeDataTypeOperation: null, detailedPanelSpec: {type:'chart', chartType: 'area'} },

      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 0,
        panelType: 'standard',
        section: 'Liabilities',
        dataTypeSpecs: [
          {
            title: 'All Liabilities',
            dataType: ['sumAccountType-loan', 'sumAccountType-credit'],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: null,
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 1,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'Home Loan',
            dataType: ['sumAccountType-loan'],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: null,
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 2,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'Credit',
            dataType: ['sumAccountType-credit'],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: null,
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 3,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'Real Loan(C)',
            dataType: [
              '7a10af92-4a9e-4952-bf70-f04920acc665',
              '4b0db8fa-e1a7-4e63-8293-7496743fa15d',
              '181d0e47-a2d3-49d4-94af-f9911f09830f',
              'b73231d0-31cb-4d6d-860f-33f229c03df2',
              'fa0f766f-7789-45b3-958f-957010a3acb3',
              '3a8eb313-54af-4528-bc9a-35c78b1c2638',
              '36147c3c-9c0f-4e89-a3c4-8e905356ad33',
              'bba2b76f-7a4b-432d-afd2-94125fd81591',
            ],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: 'sum',
            detailedPanelSpec: { type: 'chart', chartType: 'bar', combineSeries: true },
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 4,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'Real Loan',
            dataType: [
              '7a10af92-4a9e-4952-bf70-f04920acc665',
              '4b0db8fa-e1a7-4e63-8293-7496743fa15d',
              '181d0e47-a2d3-49d4-94af-f9911f09830f',
              'b73231d0-31cb-4d6d-860f-33f229c03df2',
              'fa0f766f-7789-45b3-958f-957010a3acb3',
              '3a8eb313-54af-4528-bc9a-35c78b1c2638',
              '36147c3c-9c0f-4e89-a3c4-8e905356ad33',
              'bba2b76f-7a4b-432d-afd2-94125fd81591',
            ],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: 'sum',
            detailedPanelSpec: { type: 'chart', chartType: 'bar', combineSeries: false },
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 5,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'Real Owe',
            dataType: [
              '7a10af92-4a9e-4952-bf70-f04920acc665',
              'sumAccountType-credit',
              '4b0db8fa-e1a7-4e63-8293-7496743fa15d',
              '181d0e47-a2d3-49d4-94af-f9911f09830f',
              'b73231d0-31cb-4d6d-860f-33f229c03df2',
              'fa0f766f-7789-45b3-958f-957010a3acb3',
              '3a8eb313-54af-4528-bc9a-35c78b1c2638',
              '36147c3c-9c0f-4e89-a3c4-8e905356ad33',
              'bba2b76f-7a4b-432d-afd2-94125fd81591',
              '4193171d-e325-4cf8-b630-de374e1f6d7a',
            ],
            expandGroups: false,
            negativeAsPositive: true,
            compositeDataTypeOperation: 'sum',
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 6,
        panelType: 'standard',
        section: 'Investments',
        dataTypeSpecs: [
          {
            title: 'Investments',
            dataType: [`rg/AccountType-investment`],
            expandGroups: true,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 7,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'Core Portfolio',
            dataType: [
              '9ad99f26-c3f7-4057-8383-0766276f0ccf',
              '15bea885-703f-4e05-96a0-ca291763f81e',
              '26d3846c-bb6e-48c4-bef2-5e3d738e3991',
              '315a3076-ef97-4279-b947-559df25ce7ee',
            ],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
            detailedPanelSpec: { type: 'chart', chartType: 'area', combineSeries: false },
          },
        ],
      },
      //{ cols: 1, rows: 1, y: 0, x: 11, panelType: 'standard', section: '', title: "ASX Shares", dataType: ['22b07428-3dcc-48fd-bee2-e66841e84c49'], expandGroups: false, negativeAsPositive: false, compositeDataTypeOperation: "sum" },
      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 8,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'Australian Shares',
            dataType: [`childrenOf:22b07428-3dcc-48fd-bee2-e66841e84c49`],
            expandGroups: true,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 1,
        x: 9,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'US Shares',
            dataType: [`childrenOf:275d5fd5-2fcf-40e6-a0b1-62357a60684a`, 'childrenOf:e6a70d3b-cc5d-48e1-8ada-c334a24bf979'],
            expandGroups: true,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
        ],
      },

      {
        cols: 1,
        rows: 1,
        y: 2,
        x: 0,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'Accounts By Type',
            dataType: ['rg/AccountsByType'],
            expandGroups: true,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
        ],
      },

      {
        cols: 1,
        rows: 1,
        y: 2,
        x: 1,
        panelType: 'standard',
        section: 'Netwealth',
        dataTypeSpecs: [
          {
            title: 'Netwealth (Account sum based)',
            dataType: [
              'sumAccountType-investment',
              'sumAccountType-asset',
              'sumAccountType-cash',
              'sumAccountType-bank',
              'sumAccountType-loan',
              'sumAccountType-credit',
              'sumAccountType-liability',
            ],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 2,
        x: 2,
        panelType: 'standard',
        section: 'Testing',
        dataTypeSpecs: [
          {
            title: 'Natwest',
            dataType: ['06d6bb0c-0e02-4305-be0b-1126d4170a90'],
            expandGroups: false,
            negativeAsPositive: false,
            compositeDataTypeOperation: 'sum',
            detailedPanelSpec: { type: 'chart', chartType: 'area', combineSeries: false },
          },
        ],
      },
      {
        cols: 1,
        rows: 1,
        y: 2,
        x: 3,
        panelType: 'standard',
        section: '',
        dataTypeSpecs: [
          {
            title: 'Netwealth - Detailed',
            dataType: ['rg/AccountsByType'],
            expandGroups: true,
            negativeAsPositive: false,
            compositeDataTypeOperation: null,
          },
        ],
      },
      //{ cols: 1, rows: 1, y: 0, x: 15, panelType: 'standard', section: 'Investments', title: "Investments", dataType: ['sumAccountType-investment'], negativeAsPositive: false, compositeDataTypeOperation: null, detailedPanelSpec: {type:'chart', chartType: 'area'} },

      // {cols: 1, rows: 1, y: 0, x: 2, panelType: 'standard', title: "Australian Shares", negativeAsPositive: false, compositeDataTypeOperation: null},
      //{cols: 1, rows: 1, y: 0, x: 3, panelType: 'standard', title: "US Shares", negativeAsPositive: false, compositeDataTypeOperation: null},
    ];
    // this.dashboard = [
    //   {cols: 1, rows: 1, y: 0, x: 0, type: 'standard', selected: false, data: 'dataB', header: 'Assets & Netwealth', title: "Investments", value: 520394, comparisonValue: 493411},
    //   {cols: 1, rows: 1, y: 0, x: 1, type: 'standard', selected: false, title: "Bank", dataType: 'sumAccountType-bank', negativeAsPositive: false},
    //   {cols: 1, rows: 1, y: 0, x: 2, type: 'standard', selected: false, data: 'dataB', title: "Australian Shares", value: 198234, comparisonValue: 193411},
    //   {cols: 1, rows: 1, y: 0, x: 3, type: 'standard', selected: false, data: 'dataB', title: "US Shares", value: 40032, comparisonValue: 45034},
    //   this.liabilityPanel,
    //   {cols: 1, rows: 1, y: 0, x: 5, type: 'standard', selected: false, data: 'dataB'},
    //   {cols: 1, rows: 1, y: 0, x: 6, type: 'standard', selected: false, data: 'dataB'},
    //   {cols: 1, rows: 1, y: 0, x: 7, type: 'standard', selected: false, data: 'dataB'},
    //   {cols: 1, rows: 1, y: 0, x: 8, type: 'standard', selected: false, data: 'dataB'},
    //   {cols: 1, rows: 1, y: 0, x: 9, type: 'standard', selected: false, data: 'dataB'},
    //   {cols: 2, rows: 1, y: 0, x: 10, type: 'standard', selected: false, data: 'dataB'},
    //   {cols: 1, rows: 1, y: 0, x: 12, type: 'standard', selected: false, data: 'dataB'},
    //   {cols: 1, rows: 1, y: 0, x: 13, type: 'standard', selected: false, data: 'dataB'},
    //   {cols: 1, rows: 1, y: 0, x: 14, type: 'standard', selected: false, data: 'dataB'},
    // ];

    this.dashboard = this.processPanelSpecs(this.panelSpecs);

    const savedPeriodDefault = this.getCookie(DashboardComponent.PERIOD_DEFAULT_COOKIE_ID);
    if (savedPeriodDefault) {
      for (let i = 0; i < this.periods.length; i++) {
        if (this.periods[i].value === savedPeriodDefault) {
          this.selectedPeriod = this.periods[i];
        }
      }
    }

    this.historyChartOptions = {
      //series: [{name: 'series1', data:[{x: '2020-06-01', y: 23},{x: '2020-07-01', y: 54},{x: '2020-08-01', y: 24},{x: '2020-09-01', y: 3},{x: '2020-10-01', y: 43},{x: '2020-11-01', y: 15}]}],
      series: [],
      chart: {
        height: 'auto',
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
    };

    //this.selectedResourceIds = ['4b0db8fa-e1a7-4e63-8293-7496743fa15d'];
    this.loadResources();
    this.loadAccountIndicators();
  }

  processPanelSpecs(panelSpecs: Array<PanelSpec>): Array<GridsterItem> {
    let items: Array<GridsterItem> = [];
    let maxX = 0,
      maxY = 0;
    for (let i = 0; i < panelSpecs.length; i++) {
      items.push({
        cols: panelSpecs[i].cols,
        rows: panelSpecs[i].rows,
        x: panelSpecs[i].x,
        y: panelSpecs[i].y,
        type: panelSpecs[i].panelType,
        selected: false,
        header: panelSpecs[i].section,
        // title: panelSpecs[i].dataTypeSpecs[0].title,
        dataTypeSpecs: panelSpecs[i].dataTypeSpecs,
        // title: panelSpecs[i].dataTypeSpecs[0].title, dataType: panelSpecs[i].dataTypeSpecs[0].dataType,
        // compositeDataTypeOperation: panelSpecs[i].dataTypeSpecs[0].compositeDataTypeOperation,
        // negativeAsPositive: panelSpecs[i].dataTypeSpecs[0].negativeAsPositive,
        // detailedPanelSpec: panelSpecs[i].dataTypeSpecs[0].detailedPanelSpec,
        // expandGroups: panelSpecs[i].dataTypeSpecs[0].expandGroups,
        // secondaryTitles: ["Hello"]
      });
      if (panelSpecs[i].x > maxX) {
        maxX = panelSpecs[i].x;
      }
      if (panelSpecs[i].y > maxY) {
        maxY = panelSpecs[i].y;
      }
    }

    // Check grid for section header issues
    // for(let x=0; x < maxX; x++) {
    //   let currentHeader = "";
    //   for(let y=0; y < maxY; y++) {
    //     if()
    //   }
    // }

    return items;
  }

  incrementBy(num: number, by: number): number {
    num = num + by;
    return num;
  }

  changedOptions() {
    if (this.options && this.options.api) {
      //this.options.api.optionsChanged();
    }
  }

  removeItem(item: any) {
    this.dashboard.splice(this.dashboard.indexOf(item), 1);
  }

  addItem() {
    let item: GridsterItem = { x: 0, y: 0, rows: 1, cols: 1 };
    this.dashboard.push(item);
  }

  panelResizeEnd() {
    // originalEvent: any, sizes: any
    // eslint-disable-next-line no-console
    // console.log('resize...');
    // @ts-ignore
    this.options!.api?.resize();
  }

  onClick(item: GridsterItem): void {
    // eslint-disable-next-line no-console
    // console.log('(DashboardComponent) click...: ' + JSON.stringify(item));

    //this.changeDetectorRef.detectChanges();
    item.selected = !item.selected;

    if (this.selectedItem != null) {
      console.log('(DashboardComponent) deselect: ' + JSON.stringify(this.selectedItem));
      this.selectedItem.selected = !this.selectedItem.selected;
      this.resizeEvent.emit(this.selectedItem);
    }

    console.log('(DashboardComponent) selecting: ' + JSON.stringify(item));
    // this.refreshItem = item;
    this.selectedItem = item;
    // this.changeDetectorRef.detectChanges();

    this.resizeEvent.emit(item);

    this.processDetailedPanel();
  }

  onChangePeriod(event: any): void {
    this.zone.run(() => {
      this.loadAccountIndicators();
      this.saveCookie(DashboardComponent.PERIOD_DEFAULT_COOKIE_ID, this.selectedPeriod.value);
    });
  }

  loadResources(): void {
    // eslint-disable-next-line no-console
    console.log('================ Loading resources...');
    this.isLoading = true;

    this.dashboardService.getResources().subscribe(
      response => {
        this.isLoading = false;
        // eslint-disable-next-line no-console
        // console.log(`Response: ${JSON.stringify(response)}`);
        // this.changeDetectorRef.markForCheck();
        this.allResources = response;
        this.expandRGsAndChildrenOfInDashboardSpecs(this.allResources);
      },
      error => {
        this.isLoading = false;
      },
    );
  }

  loadAccountIndicators(): void {
    // eslint-disable-next-line no-console
    console.log('================ Loading indicators...');
    this.isLoading = true;

    this.dashboardService.getAccountIndicators(this.selectedPeriod.value).subscribe(
      response => {
        this.isLoading = false;
        // eslint-disable-next-line no-console
        // console.log(`Response: ${JSON.stringify(response)}`);
        // this.changeDetectorRef.markForCheck();
        this.financeIndicators = response.indicators;
        // this.processAccountIndicators(response);
        this.loadPortfolioIndicators();
        // TODO - SEE IF THIS IS NEEDED
        // this.loadSecurityHoldingIndicators();
        this.loadAccountSnapshots();
        this.loadPortfolioSnapshots();
      },
      error => {
        this.isLoading = false;
      },
    );
  }

  loadPortfolioIndicators(): void {
    // eslint-disable-next-line no-console
    console.log('================ Loading portfolio indicators...');
    this.isLoadingPortfolioIndicators = true;

    this.dashboardService.getPortfolioIndicators(this.selectedPeriod.value).subscribe(
      response => {
        this.isLoadingPortfolioIndicators = false;
        // eslint-disable-next-line no-console
        // console.log(`Response: ${JSON.stringify(response)}`);
        // this.changeDetectorRef.markForCheck();
        if (response && response.indicators) {
          for (let i = 0; i < response.indicators.length; i++) {
            this.financeIndicators.push(response.indicators[i]);
          }
        }

        this.processAccountIndicators(this.financeIndicators);
        //this.processResourceTree(this.allResources);
      },
      error => {
        this.isLoadingPortfolioIndicators = false;
      },
    );
  }

  loadAccountSnapshots(): void {
    // eslint-disable-next-line no-console
    console.log('Loading account historical snapshots (caching)...');
    this.isLoadingAccountSnapshots = true;

    this.dashboardService.getAccountSnapshots(this.selectedPeriod.value).subscribe(
      response => {
        this.isLoadingAccountSnapshots = false;
        // eslint-disable-next-line no-console
        // console.log(`Response: ${JSON.stringify(response)}`);
        // this.changeDetectorRef.markForCheck();
        this.accountSnapshots = response;
        this.processDetailedPanel();
      },
      error => {
        this.isLoadingAccountSnapshots = false;
      },
    );
  }

  loadPortfolioSnapshots(): void {
    // eslint-disable-next-line no-console
    console.log('Loading portfolio historical snapshots (caching)...');
    this.isLoadingAccountSnapshots = true;

    this.dashboardService.getPortfolioSnapshots(true, this.selectedPeriod.value).subscribe(
      response => {
        this.isLoadingPortfolioSnapshots = false;
        // eslint-disable-next-line no-console
        // console.log(`Response: ${JSON.stringify(response)}`);
        // this.changeDetectorRef.markForCheck();
        this.portfolioSnapshots = response;
      },
      error => {
        this.isLoadingPortfolioSnapshots = false;
      },
    );
  }

  findResourceSnapshot(resourceId: string): FinanceResourceSnapshots | null {
    if (resourceId.startsWith(DashboardComponent.RESOURCE_GROUP_PREFIX)) {
      return this.performCompositeCalcsOnChildrenSnapshots(resourceId, 'All');
    } else {
      return this.findResourceSnapshotStandard(resourceId);
    }
  }

  findResourceSnapshotStandard(resourceId: string): FinanceResourceSnapshots | null {
    let snapshot: FinanceResourceSnapshots | null = null;
    for (let i = 0; i < this.accountSnapshots.length; i++) {
      if (this.accountSnapshots[i].id == resourceId) {
        // Found it
        // eslint-disable-next-line no-console
        // console.log(`Found '${resourceId}' in account-based snapshots`);
        snapshot = this.accountSnapshots[i];
        break;
      }
    }

    if (snapshot == null) {
      for (let i = 0; i < this.portfolioSnapshots.length; i++) {
        if (this.portfolioSnapshots[i].id == resourceId) {
          // Found it
          // eslint-disable-next-line no-console
          // console.log(`Found '${resourceId}' in portfolio-based snapshots`);
          snapshot = this.portfolioSnapshots[i];
          break;
        }
      }
    }

    return snapshot;
  }

  performCompositeCalcsOnChildrenSnapshots(resourceId: string, resourceName: string): FinanceResourceSnapshots | null {
    let children = this.findChildrenOfResource(this.allResources, resourceId, 2);
    // Now we have to replace the one dataType, with one or more.
    let childrenDataTypes: string[] = [];
    if (children) {
      for (let j = 0; j < children.length; j++) {
        childrenDataTypes.push(children[j].id);
      }
    }

    // console.log('performCompositeCalcsOnChildrenSnapshots: ' + JSON.stringify(childrenDataTypes));

    let resourceSnapshot: FinanceResourceSnapshots | null = null;

    for (let i = 0; i < childrenDataTypes.length; i++) {
      const child = childrenDataTypes[i];

      let singleResourceSnapshot: FinanceResourceSnapshots | null = this.findResourceSnapshot(child);
      //console.log("Checking child: "+ child );
      let currencyCode = singleResourceSnapshot?.currencyCode;

      if (resourceSnapshot == null) {
        resourceSnapshot = {
          id: '_calc_',
          name: resourceName,
          currencyCode: currencyCode!,
          annotations: [],
          symbol: 'SYMBOL',
          type: 'GRP_CALC',
          snapshots: [],
        };
        //console.log('Created group resource snapshot holder...');
      }

      if (singleResourceSnapshot != null) {
        this.aggregateSnapshots(resourceSnapshot, singleResourceSnapshot);
      } else {
        console.log('Cannot find snapshots for: ' + child);
      }
    }

    //let result = this.aggregateSnapshots(this.accountSnapshots, this.portfolioSnapshots, childrenDataTypes);
    return resourceSnapshot;
  }

  aggregateSnapshots(snap1: FinanceResourceSnapshots, snap2: FinanceResourceSnapshots | null) {
    if (snap2 == null) {
      console.log('Cannot find snapshot');
      return;
    }

    if (snap1.snapshots.length == 0) {
      for (let i = 0; i < snap2.snapshots.length; i++) {
        snap1.snapshots.push(structuredClone(snap2.snapshots[i]));
      }
    } else {
      let k = 0;
      for (let j = 0; j < snap1.snapshots.length; j++) {
        let found = false;
        while (!found && k < snap2.snapshots.length) {
          let date1 = new Date(snap1.snapshots[j].date);
          let date2 = new Date(snap2.snapshots[k].date);

          if (date1.getTime() === date2.getTime()) {
            found = true;
          } else if (date1.getTime() < date2.getTime()) {
            // need to move forward in j array, not k. Therefore should not be found and not increase k
            break;
          } else {
            k++;
          }
        }

        if (found) {
          if (snap2.snapshots[k].fxToLocal) {
            snap1.snapshots[j].value = snap1.snapshots[j].value + snap2.snapshots[k].value * snap2.snapshots[k].fxToLocal!;
            // if (snap1.snapshots[j].date === '2023-04-30') {
            // console.log(
            //   snap2.id +
            //     ',' +
            //     snap2.name +
            //     ',[' +
            //     snap1.id +
            //     "],'" +
            //     snap1.name +
            //     ',' +
            //     snap2.snapshots[k].value * snap2.snapshots[k].fxToLocal! +
            //     ',' +
            //     snap1.snapshots[j].value +
            //     ',' +
            //     snap2.snapshots[k].fxToLocal,
            // );
            // }
          } else {
            snap1.snapshots[j].value = snap1.snapshots[j].value + snap2.snapshots[k].value;
            // if (snap1.snapshots[j].date === '2023-04-30') {
            //   console.log(
            //     snap2.id +
            //       ',' +
            //       snap2.name +
            //       ',[' +
            //       snap1.id +
            //       "],'" +
            //       snap1.name +
            //       ',' +
            //       snap2.snapshots[k].value +
            //       ',' +
            //       snap1.snapshots[j].value +
            //       ',' +
            //       snap2.snapshots[k].fxToLocal,
            //   );
            // }
          }

          k++;
        }
      }
    }
  }

  processDetailedPanel() {
    if (this.selectedItem) {
      let series = this.processSnapshots(this.selectedItem);
      if (series) {
        this.historyChartOptions.series = series;
      }

      this.changeDetectorRef.markForCheck();
    }
  }

  processAccountIndicators(financeIndicators: FinanceIndicator[]) {
    // Set the values by datatype crawl.
    for (let i = 0; i < this.dashboard.length; i++) {
      if (this.dashboard[i].dataTypeSpecs) {
        //  && this.dashboard[i].dataType.length > 1
        let values = [];
        let comparisonValues = [];
        // Now we have to iterate through all the specs
        for (let j = 0; j < this.dashboard[i].dataTypeSpecs.length; j++) {
          let result = this.performIndicatorCompositeCalcs(
            financeIndicators,
            this.dashboard[i].dataTypeSpecs[j].dataType,
            this.dashboard[i].dataTypeSpecs[j].compositeDataTypeOperation,
          );
          if (result != null) {
            values.push(result.value);
            if (!result.naComparisonValue) {
              comparisonValues.push(result.comparisonValue);
            } else {
              comparisonValues.push(null);
            }
          }
        }

        //console.log('processAccountIndicators: ' + values.length + ", " + values);
        this.dashboard[i]['values'] = values;
        this.dashboard[i]['comparisonValues'] = comparisonValues;

        this.resizeEvent.emit(this.dashboard[i]);
      }
    }
  }

  performIndicatorCompositeCalcs(
    financeIndicators: FinanceIndicator[],
    dataTypes: string[],
    compositeDataTypeOperation: string,
  ): CompositeValues | null {
    // console.log('Performing Composite Calc for: ' + JSON.stringify(dataTypes));
    if (!compositeDataTypeOperation) {
      compositeDataTypeOperation = 'sum';
    }
    if (compositeDataTypeOperation === 'sum') {
      let result = new CompositeValues();
      for (let i = 0; i < dataTypes.length; i++) {
        if (dataTypes[i].startsWith(DashboardComponent.RESOURCE_GROUP_PREFIX)) {
          let resourceGroupResult = this.performCompositeCalcsOnChildren(financeIndicators, dataTypes[i], compositeDataTypeOperation);
          if (resourceGroupResult) {
            // console.log(
            //   dataTypes[i] +
            //     ',' +
            //     result.value +
            //     ',' +
            //     resourceGroupResult?.value! +
            //     ',' +
            //     (result.value! + resourceGroupResult?.value!) +
            //     ',rgResult',
            // );
            this.addToCompositeValues(
              result,
              resourceGroupResult?.value!,
              resourceGroupResult?.comparisonValue!,
              resourceGroupResult?.naComparisonValue,
            );
          } else {
            console.log('WARN: no resourceGroupResult');
          }
        } else {
          for (let j = 0; j < financeIndicators.length; j++) {
            let value: number | null = null,
              comparisonValue: number | null = null;
            if (financeIndicators[j].id === dataTypes[i]) {
              value = financeIndicators[j].snapshot.value;
              comparisonValue = financeIndicators[j].snapshot.comparisonValue;

              if (financeIndicators[j] && financeIndicators[j].snapshot && financeIndicators[j].snapshot.fxToLocal && value) {
                let fx = financeIndicators[j].snapshot.fxToLocal;
                value = value * fx!;
                if (comparisonValue) {
                  comparisonValue = comparisonValue * fx!;
                }
              }
              // console.log(
              //   dataTypes[i] +
              //     ',' +
              //     result.value +
              //     ',' +
              //     financeIndicators[j].snapshot.value +
              //     ',' +
              //     (result.value! + financeIndicators[j].snapshot.value) +
              //     ',fi' +
              //     ',' +
              //     financeIndicators[j].snapshot.fxToLocal +
              //     ', comparison: ' +
              //     comparisonValue,
              // );
              this.addToCompositeValues(result, value, comparisonValue, comparisonValue == null);

              // console.log("[" + financeIndicators[j].id + "]: " + financeIndicators[j].snapshot.value + ", CompositeValue= " + result.value + ", ComparisonValue=" + financeIndicators[j].snapshot.comparisonValue + ", CompositeComparisonValue= " + result.comparisonValue);
            }
          }
        }
      }
      return result;
    }

    return null;
  }

  addToCompositeValues(cv: CompositeValues, value: number, comparisonValue: number, noComparisonValue: boolean) {
    if (cv.value == null) {
      cv.value = value;
    } else if (value != null) {
      cv.value = cv.value + value;
    }
    if (cv.comparisonValue == null) {
      cv.comparisonValue = comparisonValue;
    } else if (comparisonValue != null) {
      cv.comparisonValue = cv.comparisonValue + comparisonValue;
    }

    if (noComparisonValue && cv.comparisonValue == null) {
      //console.log("Null Comparison Value: " + JSON.stringify(financeIndicators.indicators[j]));
      cv.naComparisonValue = true;
    }
  }

  performCompositeCalcsOnChildren(financeIndicators: FinanceIndicator[], dataType: string, compositeDataTypeOperation: string) {
    let children = this.findChildrenOfResource(this.allResources, dataType, 2);
    // Now we have to replace the one dataType, with one or more.
    let childrenDataTypes: string[] = [];
    if (children) {
      for (let j = 0; j < children.length; j++) {
        childrenDataTypes.push(children[j].id);
      }
    }
    let result = this.performIndicatorCompositeCalcs(financeIndicators, childrenDataTypes, compositeDataTypeOperation);
    return result;
  }

  expandRGsAndChildrenOfInDashboardSpecs(resources: FinanceNestedResource[]) {
    for (let i = 0; i < this.dashboard.length; i++) {
      let dataTypeSpecs = this.dashboard[i].dataTypeSpecs;
      for (let k = 0; k < dataTypeSpecs.length; k++) {
        let toAppend: ArrayAppend[] = [];
        let dataTypes = dataTypeSpecs[k].dataType;
        if (dataTypes) {
          for (let j = 0; j < dataTypes.length; j++) {
            // console.log("DataType[j]: " + dataTypes[j]);
            if (dataTypeSpecs[k].expandGroups) {
              if (dataTypes[j].startsWith(DashboardComponent.RESOURCE_GROUP_PREFIX)) {
                let children = this.findChildrenOfResource(resources, dataTypes[j], 2);
                // Now we have to replace the one dataType, with one or more.
                if (children) {
                  toAppend.push(new ArrayAppend(j, children));
                }
              } else if (dataTypes[j].startsWith(DashboardComponent.CHILDREN_OF_PREFIX)) {
                let children = this.findChildrenOfResource(
                  resources,
                  dataTypes[j].substring(DashboardComponent.CHILDREN_OF_PREFIX.length),
                  2,
                );
                // Now we have to replace the one dataType, with one or more.
                if (children) {
                  toAppend.push(new ArrayAppend(j, children));
                }
              }
            }
          }
        }
        this.fixDataTypeArray(toAppend, dataTypeSpecs[k].dataType);
      }
    }
  }

  fixDataTypeArray(toAppend: ArrayAppend[], dataTypes: string[]) {
    let adjustIndex = 0;
    for (const item of toAppend) {
      //console.log('Changing position ' + item.position + ' [' + dataTypes[item.position] + '] to: ' + JSON.stringify(item.values));
      let replaceItems = 1;
      for (const value of item.values) {
        dataTypes.splice(item.position + adjustIndex, replaceItems, value);
        adjustIndex++;
        replaceItems = 0; // Now just adding new items, rather than replacing the item at the position
      }
    }
  }

  findChildrenOfResource(resources: FinanceNestedResource[], resourceId: string, maxDepth: number): FinanceNestedResource[] | null {
    return this.findChildrenOfResourceRecursive(resources, resourceId, maxDepth, 0);
  }

  findChildrenOfResourceRecursive(
    resources: FinanceNestedResource[],
    resourceId: string,
    maxDepth: number,
    currentDepth: number,
  ): FinanceNestedResource[] | null {
    for (let i = 0; i < resources.length; i++) {
      let resource = resources[i];
      //console.log("Checking:  " + resource.id + " against: " + resourceId);
      if (resource.id === resourceId) {
        // Found!. Return Children
        return this.expandChildren(resource.children);
      } else if (currentDepth < maxDepth && resource.children && resource.children.length > 0) {
        let result = this.findChildrenOfResourceRecursive(resource.children, resourceId, maxDepth, currentDepth + 1);
        if (result) {
          return result;
        }
      }
    }
    return null;
  }

  expandChildren(resources: FinanceNestedResource[]): FinanceNestedResource[] | null {
    let result: FinanceNestedResource[] = [];
    if (resources) {
      for (let i = 0; i < resources.length; i++) {
        let resource = resources[i];
        if (resource.children && resource.children.length > 0) {
          let sub = this.expandChildren(resource.children);
          if (sub && sub.length > 0) {
            result = result.concat(sub);
          }
        } else {
          if (resource.type !== 'investment') {
            // Don't just add investment accounts with no children since they dont have the proper value assigned (it is the assets, like stocks, in them have the value)
            result = result.concat(resource);
          }
        }
      }
    }
    return result;
  }

  processSnapshots(item: GridsterItem): ApexAxisChartSeries | null {
    let resourceIds: string[] | null = null;
    if (item != null) {
      resourceIds = item.dataTypeSpecs[0].dataType;
      let ct = item.dataTypeSpecs[0].detailedPanelSpec?.chartType;
      if (!ct) {
        ct = 'bar';
      }

      if (this.historyChartOptions.chart!.type !== ct) {
        this.historyChartOptions.chart!.stackType = 'normal';
        this.historyChartOptions.chart!.stacked = true;
        this.historyChartOptions.chart!.type = ct as ChartType;
        if (this.historyChart) {
          this.historyChart.updateOptions(this.historyChartOptions);
        }
      }
    }

    if (resourceIds == null) {
      return null;
    }

    let serieses: ApexAxisChartSeries = [];
    for (let i = 0; i < resourceIds.length; i++) {
      let xy: [any?] = [];
      let series = { name: resourceIds[i], data: xy };
      let atLeastOne = false;

      let resourceSnapshot: FinanceResourceSnapshots | null = this.findResourceSnapshot(resourceIds[i]);
      if (resourceSnapshot != null && resourceSnapshot.snapshots != null) {
        if (resourceSnapshot.name != null) {
          series.name = resourceSnapshot.name;
        }
        // Add value
        let lastKnownRateToBase = null;
        for (let k = 0; k < resourceSnapshot.snapshots.length; k++) {
          atLeastOne = true;
          if (resourceSnapshot.snapshots[k].fxToLocal) {
            lastKnownRateToBase = resourceSnapshot.snapshots[k].fxToLocal;
          }
          let value = resourceSnapshot.snapshots[k].value;
          if (lastKnownRateToBase) {
            value = value * lastKnownRateToBase;
          }

          //xy.push({ x: new Date(resourceSnapshot.snapshots[k].date).getTime(), y: value });
          xy.push({ x: resourceSnapshot.snapshots[k].date, y: value });
        }
      }

      if (atLeastOne) {
        serieses.push(series);
      }
    }

    let combineSeries = item.dataTypeSpecs[0].detailedPanelSpec?.combineSeries;
    if (combineSeries) {
      serieses = this.combineSeries(serieses, 'Combined');
    }
    return serieses;
  }

  combineSeries(inputSerieses: ApexAxisChartSeries, name: string) {
    let serieses: ApexAxisChartSeries = [];
    let xy: [any?] = [];
    let series = { name: name, data: xy };
    serieses.push(series);

    // Combine
    for (let i = 0; i < inputSerieses.length; i++) {
      let currentSeriesXY = inputSerieses[i].data;
      if (currentSeriesXY) {
        for (let j = 0; j < currentSeriesXY.length; j++) {
          let item = currentSeriesXY[j] as any;
          if (xy[j]) {
            if (item) {
              xy[j].y = xy[j].y + item.y;
            }
          } else {
            xy.push(item);
          }
        }
      }
    }

    return serieses;
  }

  getCookie(key: string): any {
    return this.cookieService.get(key);
  }

  saveCookie(key: string, value: any): void {
    return this.cookieService.put(key, value);
  }

  currencyFormatterMethod(element: any): string {
    return this.currencyFormatter(element.value);
  }

  currencyFormatter(value: any): string {
    let code: string | undefined;
    // if(this.account !== null ){
    //   code = this.account.currencyCode;
    // }
    if (code === undefined) {
      // TODO Fix hard coding here
      code = 'AUD';
    }

    //  = this.currencyPipe.transform(, 'symbol-narrow'
    const val = formatCurrency(value, 'en-AU', '$', 'AUD');
    if (val === null) {
      // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
      return '' + value;
    }

    return val;
  }
}

class CompositeValues {
  value: number | null;
  comparisonValue: number | null;
  naComparisonValue = false;

  constructor() {
    this.value = 0;
    this.comparisonValue = 0;
  }
}

class ArrayAppend {
  position: number = -1;
  values: string[] = [];

  constructor(position: number, nestedResources: FinanceNestedResource[]) {
    this.position = position;
    for (const r of nestedResources) {
      this.values.push(r.id);
    }
  }
}
