import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EventEmitter } from '@angular/core';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { ThemeService } from 'app/layouts/main/theme.service';
import { AccountListComponent } from './account-list.component';
import { AccountList } from './account-list.service';

describe('AccountListComponent', () => {
  let component: AccountListComponent;
  let fixture: ComponentFixture<AccountListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountListComponent],
      providers: [
        {
          provide: AccountList,
          useValue: {
            get: () => of([]),
            updateFavourite: () => of({ id: 'account-1', favourite: true }),
          },
        },
        {
          provide: ThemeService,
          useValue: {
            onChange: new EventEmitter(),
          },
        },
        {
          provide: Router,
          useValue: {
            routerState: {
              snapshot: {
                root: { data: { pageTitle: 'Accounts' } },
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
