import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HistoryChartDialogComponent } from './history-chart-dialog.component';

describe('HistoryChartDialogComponent', () => {
  let component: HistoryChartDialogComponent;
  let fixture: ComponentFixture<HistoryChartDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistoryChartDialogComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HistoryChartDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
