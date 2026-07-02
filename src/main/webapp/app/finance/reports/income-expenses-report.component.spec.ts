import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { AccountList } from '../account-list/account-list.service';
import { FinanceManageDataService } from '../manage-data/finance-manage-data.service';
import { Transactions } from '../transactions/transactions.service';
import { ReportsService } from './reports.service';
import { IncomeExpensesReportComponent } from './income-expenses-report.component';

describe('IncomeExpensesReportComponent', () => {
  let component: IncomeExpensesReportComponent;
  let fixture: ComponentFixture<IncomeExpensesReportComponent>;

  const reportsServiceMock = {
    getDefinitions: vitest.fn(() =>
      of([
        {
          key: 'income-expenses',
          title: 'Income & Expenses',
          description: 'test',
          defaultConfig: {
            reportKey: 'income-expenses',
            name: 'Default',
            title: 'Income & Expenses',
            rowDimension: 'subcategory',
            columnDimension: 'month',
            showPercentOfTotal: false,
            defaultView: 'report',
            datePreset: '6M',
            startDate: null,
            endDate: null,
            builtin: true,
            editable: false,
            accountIds: [],
            categoryIds: [],
            payeeIds: [],
            familyMemberIds: [],
          },
        },
      ]),
    ),
    getConfigs: vitest.fn(() => of([])),
    createConfig: vitest.fn(),
    updateConfig: vitest.fn(),
    runIncomeExpenses: vitest.fn(() =>
      of({
        title: 'Income & Expenses',
        currencyCode: 'AUD',
        rowDimension: 'subcategory',
        columnDimension: 'month',
        defaultView: 'report',
        showPercentOfTotal: false,
        startDate: '2026-01-01',
        endDate: '2026-06-30',
        grandTotal: 1500,
        columns: [{ key: 'month:2026-06-01', label: 'Jun 2026', startDate: '2026-06-01', endDate: '2026-06-30', total: 1500 }],
        rows: [
          {
            key: 'row-1',
            label: 'Salary',
            groupLabel: 'Income',
            parentLabel: 'Income',
            subtotal: false,
            grandTotal: false,
            rowType: 'row',
            total: 1500,
            percentOfTotal: 1,
            values: [1500],
            valuePercents: [1],
          },
        ],
        series: [{ name: 'Salary', data: [1500] }],
        pie: [{ label: 'Salary', value: 1500, percentOfTotal: 1 }],
      }),
    ),
  };

  const accountListMock = {
    getSimple: vitest.fn(() => of([{ id: 'acc-1', name: 'Everyday', currencyCode: 'AUD' }])),
  };

  const manageDataMock = {
    getCategories: vitest.fn(() =>
      of([
        {
          id: 'income-root',
          name: 'Income',
          displayName: 'Income',
          parentId: null,
          parentName: null,
          classificationId: 0,
          level: 0,
          comment: null,
          hasChildren: true,
        },
        {
          id: 'salary',
          name: 'Salary',
          displayName: 'Income: Salary',
          parentId: 'income-root',
          parentName: 'Income',
          classificationId: 0,
          level: 1,
          comment: null,
          hasChildren: false,
        },
      ]),
    ),
    getPayees: vitest.fn(() => of([{ id: 'payee-1', name: 'Employer', parentId: null, hidden: false, childCount: 0, childNames: [] }])),
  };

  const transactionsMock = {
    getWhoTreeOptions: vitest.fn(() =>
      of([
        {
          key: 'family-1',
          label: 'Alice',
          selectable: true,
          leaf: true,
          children: [],
        },
      ]),
    ),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IncomeExpensesReportComponent],
      providers: [
        { provide: ReportsService, useValue: reportsServiceMock },
        { provide: AccountList, useValue: accountListMock },
        { provide: FinanceManageDataService, useValue: manageDataMock },
        { provide: Transactions, useValue: transactionsMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IncomeExpensesReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the default preset with all filters selected', () => {
    const instance = component as any;

    expect(instance.workingConfig?.builtin).toBe(true);
    expect(instance.workingConfig?.rowDimension).toBe('subcategory');
    expect(instance.workingConfig?.columnDimension).toBe('month');
    expect(instance.workingConfig?.defaultView).toBe('report');
    expect(instance.workingConfig?.datePreset).toBe('6M');
    expect(instance.workingConfig?.accountIds).toEqual(['acc-1']);
    expect(instance.workingConfig?.categoryIds).toContain('salary');
    expect(instance.workingConfig?.payeeIds).toEqual(['payee-1']);
    expect(instance.workingConfig?.familyMemberIds).toEqual(['family-1']);
    expect(reportsServiceMock.runIncomeExpenses).toHaveBeenCalled();
  });

  it('requires save as when the active config is built in', () => {
    const instance = component as any;
    instance.saveCurrentConfig();

    expect(instance.saveDialogVisible).toBe(true);
    expect(instance.saveDialogMode).toBe('saveAs');
  });
});
