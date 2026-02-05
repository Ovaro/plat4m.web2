import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  ViewEncapsulation,
} from '@angular/core';

import { GridsterItem } from 'angular-gridster2';
import { Subscription } from 'rxjs';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { SharedModule } from 'app/shared/shared.module';
import { DashboardService } from './dashboard.service';
import { TooltipModule } from 'primeng/tooltip';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'standard-dashboard-panel',
  templateUrl: './standard.panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  standalone: true,
  imports: [CurrencyPipe, SharedModule, TooltipModule, SkeletonModule],
})
export class StandardPanelComponent implements OnInit, OnDestroy, OnChanges {
  @Input()
  widget: any;
  @Input()
  resizeEvent: any;
  // resizeSub: Subscription;
  @Input()
  isLoading = false;

  resizeSub: Subscription | null = null;

  defaultCurrencyCode = 'AUD';

  constructor(
    private dashboardService: DashboardService,
    private changeDetectorRef: ChangeDetectorRef,
  ) {
    //this.resizeSub = {};
  }

  ngOnInit(): void {
    this.resizeSub = this.resizeEvent.subscribe((widget: any) => {
      if (widget === this.widget) {
        // or check id , type or whatever you have there
        // resize your widget, chart, map , etc.
        // console.log("resizeSub:" + widget);
        this.changeDetectorRef.markForCheck();
      }
    });
    if (this.widget && this.widget.dataType) {
      //this.load();
      //this.widget.value = '-';
    }
  }

  ngOnChanges() {
    // eslint-disable-next-line no-console
    // console.log(`ngOnChanges`);
  }

  load(): void {
    // eslint-disable-next-line no-console
    console.log('Loading snapshot');
    this.isLoading = true;

    this.dashboardService.getSnapshot(this.widget.dataType, this.widget.dataClass, this.widget.periodAgo).subscribe(
      response => {
        this.isLoading = false;
        // eslint-disable-next-line no-console
        console.log(`Response: ${JSON.stringify(response)}`);
        this.changeDetectorRef.markForCheck();

        this.widget.value = response.value;
        this.widget.comparisonValue = response.comparisonValue;
        // if(this.widget.negativeAsPositive) {
        //   this.widget.value = this.widget.value * -1;
        //   this.widget.comparisonValue = this.widget.comparisonValue *-1;
        // }
        console.log(`** Widget:`, JSON.stringify(this.widget));
      },
      error => {
        this.handleError(error);
      },
    );
  }

  handleError(error: any): void {
    this.isLoading = false;
    // Handle the error state here
    console.error('An error occurred:', error);
    // You can show an error message to the user or perform any other error handling logic
  }

  ngOnDestroy(): void {
    //   this.resizeSub.unsubscribe();
  }
}
