import { EventEmitter } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { themeAlpine } from 'ag-grid-community';
import { TransactionsComponent } from './transactions.component';
import { Transactions } from './transactions.service';
import { ThemeService } from 'app/layouts/main/theme.service';
import { AccountList } from '../account-list/account-list.service';
import { CookieService } from 'ngx-cookie';
import { AgGridThemeService } from 'app/shared/ag-grid/ag-grid-theme.service';

describe('TransactionsComponent', () => {
  let component: TransactionsComponent;
  let fixture: ComponentFixture<TransactionsComponent>;

  const transactionsServiceMock = {
    get: () => of(new HttpResponse({ body: [], headers: new HttpHeaders({ 'X-Total-Count': '0' }) })),
    getEditorOptions: () => of({ categories: [], payees: [] }),
    getLinkedTransfer: () =>
      of({
        id: 'txn-2',
        accountId: 'account-2',
        date: '2026-01-02',
        payeeName: 'Transfer from : Everyday',
        payeeId: '',
        memo: null,
        amount: 25,
        runningBalance: 200,
        categoryId: '',
        categoryName: '',
        parentCategoryId: '',
        parentCategoryName: '',
        splitParent: false,
        splitChild: false,
        transferredAccountId: 'account-1',
        cleared: false,
        voided: false,
        number: 1,
        payment: 0,
        deposit: 25,
        displayCategory: '',
        name: '',
        type: 0,
      }),
    update: () => of(),
    create: () => of(),
  };

  const themeServiceMock = {
    theme: () => 'light',
    onChange: new EventEmitter(),
  };

  const accountListMock = {
    getSimple: () => of([]),
    updateFavourite: () => of({ id: 'account-1', favourite: true }),
  };

  const cookieServiceMock = {
    get: () => null,
    put: () => undefined,
  };

  const agGridThemeServiceMock = {
    theme: () => 'alpine',
    onChange: new EventEmitter(),
    getThemeDefinition: () => themeAlpine,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionsComponent],
      providers: [
        { provide: Transactions, useValue: transactionsServiceMock },
        { provide: ThemeService, useValue: themeServiceMock },
        { provide: AccountList, useValue: accountListMock },
        { provide: CookieService, useValue: cookieServiceMock },
        { provide: AgGridThemeService, useValue: agGridThemeServiceMock },
        { provide: ActivatedRoute, useValue: { params: of({ id: 'account-1' }) } },
        {
          provide: Router,
          useValue: {
            navigate: () => Promise.resolve(true),
            routerState: {
              snapshot: {
                root: { data: { pageTitle: 'Transactions' } },
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('opens the editor when a transaction row is clicked', () => {
    const setSelected = jasmine.createSpy('setSelected');

    component.onCellClicked({
      data: {
        id: 'txn-1',
        date: '2026-01-02',
        payeeName: 'Grocer',
        payeeId: 'payee-1',
        memo: 'Weekly shop',
        amount: -55.25,
        runningBalance: 1000,
        categoryId: 'cat-1',
        categoryName: 'Food',
        parentCategoryId: 'cat-parent',
        parentCategoryName: 'Living',
        splitParent: false,
        splitChild: false,
        transferredAccountId: '',
        cleared: true,
        voided: false,
        number: 1,
        payment: 0,
        deposit: 0,
        displayCategory: '',
        name: '',
        type: 0,
      },
      node: { setSelected },
    } as any);

    expect(component.isEditorOpen).toBe(true);
    expect(component.editorMode).toBe('view');
    expect(component.selectedTransactionId).toBe('txn-1');
    expect(component.selectedTransaction?.displayCategory).toBe('Living: Food');
    expect(setSelected).toHaveBeenCalledWith(true, true);
  });

  it('opens add mode from the add action', () => {
    component.accountId = 'account-1';
    component.account = {
      id: 'account-1',
      name: 'Everyday',
      type: 1,
      accountType: 'Bank',
      currencyCode: 'AUD',
      balance: 0,
      balanceWarning: '',
      fxRateToLocal: null,
      fxDateTime: null,
      relatedToAccountId: null,
      closed: false,
      favourite: false,
      institution: null,
      startingBalance: 100,
    };

    component.openAddTransaction();

    expect(component.isEditorOpen).toBe(true);
    expect(component.editorMode).toBe('add');
    expect(component.selectedTransactionId).toBeNull();
    expect(component.selectedTransaction?.date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('maps ag-grid sort state into backend sort parameters', () => {
    const query = component['buildGridQuery']({
      startRow: 100,
      sortModel: [{ colId: 'payee', sort: 'asc' }],
    } as any);

    expect(query).toEqual({
      page: 1,
      size: 100,
      sort: ['payeeName,asc'],
    });
  });

  it('does not mark transfer transactions as readonly', () => {
    component.selectedTransaction = {
      id: 'txn-1',
      date: '2026-01-02',
      payeeName: 'Transfer to : Savings',
      payeeId: '',
      memo: null,
      amount: -25,
      runningBalance: 100,
      categoryId: '',
      categoryName: '',
      parentCategoryId: '',
      parentCategoryName: '',
      splitParent: false,
      splitChild: false,
      transferredAccountId: 'account-2',
      cleared: false,
      voided: false,
      number: 1,
      payment: 25,
      deposit: 0,
      displayCategory: '',
      name: '',
      type: 0,
    };

    expect(component.getSelectedTransactionReadonlyReason()).toBeNull();
  });

  it('formats transfer payees with account context and payee name', () => {
    component.accounts = [
      {
        id: 'account-1',
        name: 'Everyday',
        type: 1,
        accountType: 'Bank',
        currencyCode: 'AUD',
        balance: 0,
        balanceWarning: '',
        fxRateToLocal: null,
        fxDateTime: null,
        relatedToAccountId: null,
        closed: false,
        favourite: false,
        institution: null,
        startingBalance: 0,
      },
      {
        id: 'account-2',
        name: 'Visa Card',
        type: 1,
        accountType: 'Credit',
        currencyCode: 'AUD',
        balance: 0,
        balanceWarning: '',
        fxRateToLocal: null,
        fxDateTime: null,
        relatedToAccountId: null,
        closed: false,
        favourite: false,
        institution: null,
        startingBalance: 0,
      },
    ];

    const payeeColumn = component.columnDefs.find(column => 'colId' in column && column.colId === 'payee') as any;
    const displayValue = payeeColumn.valueGetter({
      data: {
        id: 'txn-1',
        date: '2026-01-02',
        payeeName: 'Electricity Bill',
        payeeId: 'payee-1',
        memo: null,
        amount: -120,
        runningBalance: 100,
        categoryId: '',
        categoryName: '',
        parentCategoryId: '',
        parentCategoryName: '',
        splitParent: false,
        splitChild: false,
        transferredAccountId: 'account-2',
        cleared: false,
        voided: false,
        number: 1,
        payment: 120,
        deposit: 0,
        displayCategory: '',
        name: '',
        type: 0,
      },
    });

    expect(displayValue).toBe('Transfer to: Visa Card (Electricity Bill)');
  });
});
