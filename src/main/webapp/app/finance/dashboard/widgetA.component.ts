import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { GridsterItem } from 'angular-gridster2';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-widget-a',
  templateUrl: './widgetA.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  standalone: true,
})
export class WidgetAComponent implements OnInit, OnDestroy {
  @Input()
  widget: any;
  @Input()
  resizeEvent: any;
  // @Input()
  // resizeEvent: EventEmitter<GridsterItem>;
  // resizeSub: Subscription;

  constructor() {
    //this.resizeSub = {};
  }

  ngOnInit(): void {
    //   this.resizeSub = this.resizeEvent.subscribe(widget => {
    //     if (widget === this.widget) {
    //       // or check id , type or whatever you have there
    //       // resize your widget, chart, map , etc.
    //       console.log(widget);
    //     }
    //   });
  }

  ngOnDestroy(): void {
    //   this.resizeSub.unsubscribe();
  }
}
