import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, ViewEncapsulation } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { StandardPanelComponent } from './standard.panel.component';
import { WidePanelComponent } from './wide.panel.component';
import { WidgetAComponent } from './widgetA.component';

@Component({
  selector: 'app-parent-dynamic',
  templateUrl: './parentDynamic.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  standalone: true,
  imports: [CommonModule, WidgetAComponent, StandardPanelComponent, WidePanelComponent],
})
export class ParentDynamicComponent implements OnChanges {
  @Input()
  widget: any;

  @Input()
  resizeEvent: any;

  resizeSub: Subscription | null = null;

  //   @Input() events: Observable<void>;

  constructor(private changeDetectorRef: ChangeDetectorRef) {}

  ngOnChanges() {
    // eslint-disable-next-line no-console
    // console.log(`ngOnChanges`);
    this.resizeSub = this.resizeEvent.subscribe((widget: any) => {
      if (widget === this.widget) {
        // or check id , type or whatever you have there
        // resize your widget, chart, map , etc.
        // console.log("resizeSubParent:" + widget.selected + ", this.widget.selected = " + this.widget.selected);

        this.changeDetectorRef.detectChanges();
        this.widget = widget;
      }
    });
  }

  repaint() {
    this.changeDetectorRef.detectChanges();
  }

  //   onClick(): void {
  //     // eslint-disable-next-line no-console
  //     console.log('(ParentDynamicComponent) click...');
  //   }
}
