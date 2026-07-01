import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AccountList } from '../account-list/account-list.service';
import { FinancialTransaction } from '../finance.model';
import { FinanceManageDataService } from '../manage-data/finance-manage-data.service';
import { ManagedPayee } from '../manage-data/finance-manage-data.types';
import { PayeesComponent } from './payees.component';

describe('PayeesComponent', () => {
  let component: PayeesComponent;
  let fixture: ComponentFixture<PayeesComponent>;

  const visiblePayees: ManagedPayee[] = [
    { id: 'parent-1', name: 'Woolworths', parentId: null, hidden: false, childCount: 1, childNames: ['Woolies Metro'] },
    { id: 'child-1', name: 'Woolies Metro', parentId: 'parent-1', hidden: false, childCount: 0, childNames: [] },
    { id: 'parent-2', name: 'Telstra', parentId: null, hidden: false, childCount: 0, childNames: [] },
  ];
  const hiddenVariant: ManagedPayee = {
    id: 'child-2',
    name: 'Woolworths Online',
    parentId: 'parent-1',
    hidden: true,
    childCount: 0,
    childNames: [],
  };
  const allPayees: ManagedPayee[] = [
    {
      id: 'parent-1',
      name: 'Woolworths',
      parentId: null,
      hidden: false,
      childCount: 2,
      childNames: ['Woolies Metro', 'Woolworths Online'],
    },
    visiblePayees[1],
    hiddenVariant,
    visiblePayees[2],
  ];

  const manageDataServiceMock = jasmine.createSpyObj<FinanceManageDataService>('FinanceManageDataService', [
    'getPayees',
    'getPayeeTransactions',
    'createPayee',
    'updatePayee',
    'deletePayee',
  ]);
  const accountListMock = {
    getSimple: () => of([]),
  };

  beforeEach(async () => {
    manageDataServiceMock.getPayees.and.callFake((includeHidden: boolean) => of(includeHidden ? allPayees : visiblePayees));
    manageDataServiceMock.getPayeeTransactions.and.returnValue(of([]));
    manageDataServiceMock.createPayee.and.returnValue(of(visiblePayees[0]));
    manageDataServiceMock.updatePayee.and.returnValue(of(visiblePayees[0]));
    manageDataServiceMock.deletePayee.and.returnValue(of(null));

    await TestBed.configureTestingModule({
      imports: [PayeesComponent],
      providers: [
        { provide: FinanceManageDataService, useValue: manageDataServiceMock },
        { provide: AccountList, useValue: accountListMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PayeesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    manageDataServiceMock.getPayees.calls.reset();
    manageDataServiceMock.getPayeeTransactions.calls.reset();
    manageDataServiceMock.createPayee.calls.reset();
    manageDataServiceMock.updatePayee.calls.reset();
    manageDataServiceMock.deletePayee.calls.reset();
  });

  it('shows only top-level payees in the main list', () => {
    expect((component as any).filteredPayees.map((payee: ManagedPayee) => payee.id)).toEqual(['parent-1', 'parent-2']);
  });

  it('matches parent rows when a visible variant name is searched', () => {
    (component as any).searchText = 'metro';

    expect((component as any).filteredPayees.map((payee: ManagedPayee) => payee.id)).toEqual(['parent-1']);
  });

  it('creates top-level payees from the page-level add dialog', () => {
    (component as any).openAddDialog();
    (component as any).form.controls.name.setValue('New Parent');

    (component as any).save();

    expect(manageDataServiceMock.createPayee).toHaveBeenCalledWith({
      name: 'New Parent',
      parentId: null,
      hidden: false,
    });
  });

  it('loads hidden variants into the parent edit dialog', () => {
    (component as any).openEditDialog(visiblePayees[0]);

    expect(manageDataServiceMock.getPayees).toHaveBeenCalledWith(true);
    expect((component as any).dialogVariants.map((payee: ManagedPayee) => payee.id)).toEqual(['child-1', 'child-2']);
  });

  it('creates variants under the current parent payee', () => {
    (component as any).openEditDialog(visiblePayees[0]);
    (component as any).openAddVariantDialog();
    (component as any).variantForm.controls.name.setValue('Woolies Petrol');

    (component as any).saveVariant();

    expect(manageDataServiceMock.createPayee).toHaveBeenCalledWith({
      name: 'Woolies Petrol',
      parentId: 'parent-1',
      hidden: false,
    });
  });

  it('updates existing variants without exposing parent reassignment', () => {
    (component as any).openEditDialog(visiblePayees[0]);
    (component as any).openEditVariantDialog(visiblePayees[1]);
    (component as any).variantForm.controls.name.setValue('Metro Express');

    (component as any).saveVariant();

    expect(manageDataServiceMock.updatePayee).toHaveBeenCalledWith('child-1', {
      name: 'Metro Express',
      parentId: 'parent-1',
      hidden: false,
    });
  });

  it('filters payee transactions by range before computing totals', () => {
    const now = new Date();
    const recentDate = new Date(now);
    recentDate.setMonth(recentDate.getMonth() - 5);
    const oldDate = new Date(now);
    oldDate.setFullYear(oldDate.getFullYear() - 3);

    (component as any).payeeTransactions = [
      buildTransaction('recent', toDateInputValue(recentDate), 45),
      buildTransaction('old', toDateInputValue(oldDate), 10),
    ];
    (component as any).setPayeeTransactionsRange('year');

    expect((component as any).filteredPayeeTransactions.map((transaction: FinancialTransaction) => transaction.id)).toEqual(['recent']);
    expect((component as any).payeeTransactionsSum).toBe(45);
    expect((component as any).payeeTransactionsAverage).toBe(45);
  });
});

function buildTransaction(id: string, date: string, amount: number): FinancialTransaction {
  return {
    id,
    date,
    name: id,
    type: 0,
    payeeName: 'Woolworths',
    payeeId: 'parent-1',
    memo: null,
    amount,
    runningBalance: 0,
    categoryId: 'groceries',
    categoryName: 'Groceries',
    parentCategoryId: '',
    parentCategoryName: '',
    splitParent: false,
    splitChild: false,
    transferredAccountId: '',
    cleared: true,
    voided: false,
    number: 0,
    payment: amount < 0 ? Math.abs(amount) : 0,
    deposit: amount > 0 ? amount : 0,
    displayCategory: 'Groceries',
    tags: [],
    tagsDisplay: '',
    whoId: null,
    whoName: null,
    accountId: null,
    importId: null,
  };
}

function toDateInputValue(date: Date): string {
  return date.toISOString().slice(0, 10);
}
