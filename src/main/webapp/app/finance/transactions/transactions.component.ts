import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Component, Inject, LOCALE_ID, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import {
  AllCommunityModule,
  CellClickedEvent,
  ClientSideRowModelModule,
  ColDef,
  GridOptions,
  GridReadyEvent,
  IDatasource,
  IGetRowsParams,
  InfiniteRowModelModule,
  IServerSideDatasource,
  IServerSideGetRowsParams,
  ModelUpdatedEvent,
  Module,
  ModuleRegistry,
  RowModelType,
  RowNode,
} from 'ag-grid-community';
import { ThemeChangeEvent, ThemeService } from 'app/layouts/main/theme.service';
import { Transactions } from './transactions.service';
import { FinancialAccount, FinancialTransaction } from '../finance.model';
import { formatCurrency } from '@angular/common';
import { Pagination } from 'app/core/request/request.model';
import { AccountList } from '../account-list/account-list.service';
import { CookieService } from 'ngx-cookie';
import SharedModule from 'app/shared/shared.module';
import { NgApexchartsModule } from 'ng-apexcharts';

@Component({
  selector: 'jhi-transactions',
  templateUrl: './transactions.component.html',
  styleUrls: ['./transactions.component.scss'],
  imports: [SharedModule, NgApexchartsModule, AgGridAngular],
})
export class TransactionsComponent {
  // implements OnInit

  static PREV_TXN_ACCOUNT_ID = 'previous-txn-account-id';
  title = '';
  theme = 'light';
  isLoading = false;
  accountId: string | null = null;
  account: FinancialAccount | null = null;

  totalItems = 0;
  itemsPerPage = 100;
  page!: number;
  predicate!: string;
  ascending!: boolean;

  balance = -1;

  // Each Column Definition results in one Column.
  public columnDefs: ColDef[] = [
    // { field: 'Flag', headerComponentParams: { template: '<span class="logo-img"></span>'}},
    { field: 'date', headerName: 'Date' },
    {
      colId: 'payee',
      field: 'payeeName',
      headerName: 'Payee',
      colSpan: params => (params.data?.payeeName?.startsWith('Transfer ') ? 2 : 1),
    },
    { colId: 'category', field: 'displayCategory', headerName: 'Category' },
    { field: 'Tags' },
    {
      field: 'payment',
      headerName: 'Payment',
      type: 'numericColumn',
      valueFormatter: params => {
        if (!params.value) return '';
        return formatCurrency(params.value, 'en-AU', '$', 'AUD');
      },
    }, //, valueFormatter: this.currencyFormatterMethod.bind(this)
    {
      field: 'deposit',
      headerName: 'Deposit',
      type: 'numericColumn',
      valueFormatter: params => {
        if (!params.value) return '';
        return formatCurrency(params.value, 'en-AU', '$', 'AUD');
      },
    }, // , valueFormatter: this.currencyFormatterMethod.bind(this)
    // { field: 'amount', headerName: 'Amount', type: 'numericColumn', valueFormatter: this.currencyFormatterMethod },
    {
      field: 'runningBalance',
      headerName: 'Balance',
      type: 'numericColumn',
      valueFormatter: params => {
        if (!params.value) return '';
        return formatCurrency(params.value, 'en-AU', '$', 'AUD');
      },
    }, // , valueFormatter: this.currencyFormatterMethod.bind(this)
  ];

  // DefaultColDef sets props common to all Columns
  public defaultColDef: ColDef = {
    sortable: true,
    filter: true,
    flex: 1,
    resizable: true,
    wrapText: false,
    autoHeight: true,
  };

  // Data that gets displayed in the grid
  //public rowData$!: Observable<FinancialTransactions[]>;
  public transactions!: FinancialTransaction[] | null;

  public accounts!: FinancialAccount[] | null;

  // For accessing the Grid's API
  @ViewChild(AgGridAngular) agGrid!: AgGridAngular;

  rowModelType: RowModelType = 'infinite';

  gridOptions: GridOptions = {
    pagination: false,
    rowSelection: 'multiple',
    rowModelType: this.rowModelType,
    // cacheBlockSize: this.itemsPerPage, // you can have your custom page size
    paginationPageSize: this.itemsPerPage,
  };

  dataSource: IDatasource = {
    getRows: (params: IGetRowsParams) => {
      // Use startRow and endRow for sending pagination to Backend
      // params.startRow : Start Page
      // params.endRow : End Page

      //replace this.apiService with your Backend Call that returns an Observable
      if (this.accountId) {
        const page = params.endRow / this.itemsPerPage - 1;
        const req: Pagination = { page, size: this.itemsPerPage, sort: [] };
        this.transactionService.get(this.accountId, req).subscribe({
          next: (res: HttpResponse<FinancialTransaction[]>) => {
            this.isLoading = false;
            this.onSuccess(res.body, res.headers);
            let lastRow = -1;
            const data = res.body!;
            if (data.length <= params.endRow) {
              lastRow = data.length;
            }
            params.successCallback(data, this.totalItems);
          },
          error: () => (this.isLoading = false),
        });
      }
    },
    rowCount: undefined,
  };

  constructor(
    private transactionService: Transactions,
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private themeService: ThemeService,
    private accountService: AccountList,
    private cookieService: CookieService,
  ) {
    // Do nothing
    //this.moneyPipe = new CurrencyPipe(_locale, "AUD");
  }

  ngOnInit(): void {
    this.theme = this.themeService.theme();
    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;
    });

    this.title = this.getPageTitle(this.router.routerState.snapshot.root);

    this.route.params.subscribe(params => {
      if (params['id']) {
        this.accountId = params['id'];
      }
    });

    this.accountService.getSimple().subscribe({
      next: (res: FinancialAccount[]) => {
        this.accounts = res;
        if (this.accountId != null) {
          this.account = this.getAccount(this.accountId);
        }
      },
      // error: () => (),
    });

    if (this.accountId == null) {
      const prevTxnAccountId = this.getCookie(TransactionsComponent.PREV_TXN_ACCOUNT_ID);
      if (prevTxnAccountId) {
        this.accountId = prevTxnAccountId;
      }
    }
  }

  onChangeAccount(event: any): void {
    this.balance = -1;
    if (this.accountId) {
      this.account = this.getAccount(this.accountId);
      this.agGrid.api.setGridOption('datasource', this.dataSource);
      this.saveCookie(TransactionsComponent.PREV_TXN_ACCOUNT_ID, this.accountId);
    }
  }

  getAccountName(id: string): string {
    let val = '[UNKNOWN]';
    this.accounts?.every(element => {
      if (element.id === id) {
        val = element.name;
        return false;
      }
      return true;
    });
    return val;
  }

  getAccount(id: string): FinancialAccount | null {
    let val = null;
    this.accounts?.every(element => {
      if (element.id === id) {
        val = element;
        return false;
      }
      return true;
    });
    return val;
  }

  // categoryFormatterMethod(element: any): string  {

  //   if(val === null) {
  //     return "";
  //   }

  //   return val;
  // }

  currencyFormatterMethod(element: any): string {
    // const val = "$" + String(element.value);
    //  eslint-disable-next-line no-console

    let code: string | undefined;
    if (this.account !== null) {
      code = this.account.currencyCode;
    }
    if (code === undefined) {
      // TODO Fix hard coding here
      code = 'AUD';
    }

    const val = formatCurrency(element.value, 'en-AU', '$', 'AUD');
    if (val === null) {
      return '';
    }

    return val;
    // const sansDec = element.value.toFixed(0);
    // const formatted = sansDec.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    // return "$" + String(formatted);
  }

  // Example load data from sever
  onGridReady(params: GridReadyEvent): void {
    // if(this.accountId) {
    //   this.transactionService.get(this.accountId).subscribe({
    //     next: (res: HttpResponse<FinancialTransaction[]>) => {
    //       this.isLoading = false;
    //       this.onSuccess(res.body, res.headers);
    //     },
    //     error: () => (this.isLoading = false),
    //   });
    //   params.api.paginationSetPageSize(this.itemsPerPage);
    // }
    // }

    //this.rowData$ = this.transactionService.get(this.accountId!);
    params.api.setGridOption('datasource', this.dataSource);
  }

  onModelUpdated(event: ModelUpdatedEvent<any>): void {
    this.autoSize();
  }

  // Example of consuming Grid Event
  onCellClicked(e: CellClickedEvent): void {
    // eslint-disable-next-line no-console
    console.log('cellClicked', e);
  }

  // Example using Grid's API
  clearSelection(): void {
    this.agGrid.api.deselectAll();
  }

  autoSize(): void {
    //ZZ
    // this.agGrid.api.autoSizeColumns(["payee","category"], false);
  }

  adjustedBalanceForCurrency(balance: number): number {
    // TODO: Fix hard coding of local currency here
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    if (this.account?.currencyCode !== 'AUD' && this.account?.fxRateToLocal != null) {
      // Convert
      return balance * this.account.fxRateToLocal;
    }

    return balance;
  }

  private onSuccess(transactions: FinancialTransaction[] | null, headers: HttpHeaders): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    //this.rowData$ = Observable.create(transactions);
    this.processTransactions(transactions!);
  }

  private processTransactions(transactions: FinancialTransaction[]): void {
    transactions.forEach(element => {
      if (element.splitParent) {
        element.displayCategory = 'Split / Multiple Cateogries';
      } else if (element.categoryName) {
        element.displayCategory = element.categoryName;
        if (element.parentCategoryName) {
          element.displayCategory = String(element.displayCategory) + ': ' + String(element.parentCategoryName);
        }
      }

      if (element.transferredAccountId) {
        if (element.amount >= 0) {
          element.payeeName = 'Transfer from : ' + this.getAccountName(element.transferredAccountId);
        } else {
          element.payeeName = 'Transfer to : ' + this.getAccountName(element.transferredAccountId);
        }
      }

      // Adjust for starting balance
      if (this.account?.startingBalance !== 0) {
        // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
        element.runningBalance = element.runningBalance + this.account!.startingBalance;
      }

      if (element.amount >= 0) {
        element.deposit = element.amount;
      } else {
        element.payment = element.amount;
      }

      //element.balance = this.currencyFormatter.transform(element.balance, 'symbol-narrow');
    });

    this.transactions = transactions;

    if (this.balance === -1 && transactions.length > 0) {
      this.balance = transactions[0].runningBalance;
    } else if (this.balance === -1 && transactions.length === 0 && this.account != null) {
      this.balance = this.account.startingBalance;
    }
  }

  private getPageTitle(routeSnapshot: ActivatedRouteSnapshot): string {
    let title: string = routeSnapshot.data['pageTitle'] ? routeSnapshot.data['pageTitle'] : 'App';
    if (routeSnapshot.firstChild) {
      title = this.getPageTitle(routeSnapshot.firstChild) || title;
    }
    // // console.log("Page Title (NAV): " + title);
    if (routeSnapshot.firstChild) {
      title = this.getPageTitle(routeSnapshot.firstChild) || title;
      // // console.log("Page Title (from FirstChild): " + title);
    }
    return title;
  }

  getCookie(key: string): any {
    return this.cookieService.get(key);
  }

  saveCookie(key: string, value: any): void {
    return this.cookieService.put(key, value);
  }
}
