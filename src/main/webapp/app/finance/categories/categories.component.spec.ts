import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AccountList } from '../account-list/account-list.service';
import { FinancialTransaction } from '../finance.model';
import { FinanceManageDataService } from '../manage-data/finance-manage-data.service';
import { ManagedCategory } from '../manage-data/finance-manage-data.types';
import { CategoriesComponent } from './categories.component';

describe('CategoriesComponent', () => {
  let component: CategoriesComponent;
  let fixture: ComponentFixture<CategoriesComponent>;

  const categories: ManagedCategory[] = [
    {
      id: 'groceries',
      name: 'Groceries',
      displayName: 'Groceries',
      classificationId: 0,
      parentId: null,
      parentName: null,
      level: 0,
      hasChildren: false,
      comment: null,
    },
  ];

  const manageDataServiceMock = jasmine.createSpyObj<FinanceManageDataService>('FinanceManageDataService', [
    'getCategories',
    'getCategoryTransactions',
    'createCategory',
    'updateCategory',
    'deleteCategory',
  ]);
  const accountListMock = {
    getSimple: () => of([]),
  };

  beforeEach(async () => {
    manageDataServiceMock.getCategories.and.returnValue(of(categories));
    manageDataServiceMock.getCategoryTransactions.and.returnValue(of([]));
    manageDataServiceMock.createCategory.and.returnValue(of(categories[0]));
    manageDataServiceMock.updateCategory.and.returnValue(of(categories[0]));
    manageDataServiceMock.deleteCategory.and.returnValue(of(null));

    await TestBed.configureTestingModule({
      imports: [CategoriesComponent],
      providers: [
        { provide: FinanceManageDataService, useValue: manageDataServiceMock },
        { provide: AccountList, useValue: accountListMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('filters category transactions by range before computing totals', () => {
    const now = new Date();
    const recentDate = new Date(now);
    recentDate.setMonth(recentDate.getMonth() - 2);
    const oldDate = new Date(now);
    oldDate.setFullYear(oldDate.getFullYear() - 2);

    (component as any).categoryTransactions = [
      buildTransaction('recent', toDateInputValue(recentDate), -25),
      buildTransaction('old', toDateInputValue(oldDate), -100),
    ];
    (component as any).setCategoryTransactionsRange('year');

    expect((component as any).filteredCategoryTransactions.map((transaction: FinancialTransaction) => transaction.id)).toEqual(['recent']);
    expect((component as any).categoryTransactionsSum).toBe(-25);
    expect((component as any).categoryTransactionsAverage).toBe(-25);
  });
});

function buildTransaction(id: string, date: string, amount: number): FinancialTransaction {
  return {
    id,
    date,
    name: id,
    type: 0,
    payeeName: 'Payee',
    payeeId: 'payee',
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
