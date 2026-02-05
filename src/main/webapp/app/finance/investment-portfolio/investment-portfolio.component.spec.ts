import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InvestmentPortfolioComponent } from './investment-portfolio.component';

describe('InvestmentPortfolioComponent', () => {
  let component: InvestmentPortfolioComponent;
  let fixture: ComponentFixture<InvestmentPortfolioComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InvestmentPortfolioComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(InvestmentPortfolioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
