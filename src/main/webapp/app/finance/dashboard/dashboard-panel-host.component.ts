import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewEncapsulation } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { ParentDynamicComponent } from './parentDynamic.component';
import { DashboardGridItem } from './dashboard.types';

@Component({
  selector: 'jhi-dashboard-panel-host',
  template: `
    <div class="dashboard-panel-host" (click)="panelClick.emit(widget)">
      <button
        type="button"
        class="dashboard-panel-host__menu"
        aria-label="Configure panel"
        title="Configure panel"
        (pointerdown)="$event.stopPropagation()"
        (mousedown)="$event.stopPropagation()"
        (touchstart)="$event.stopPropagation()"
        (click)="onConfigureClick($event)"
      >
        <fa-icon icon="ellipsis-h"></fa-icon>
      </button>
      <app-parent-dynamic [widget]="widget" [resizeEvent]="resizeEvent"></app-parent-dynamic>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [FontAwesomeModule, ParentDynamicComponent],
})
export class DashboardPanelHostComponent {
  @Input({ required: true }) widget!: DashboardGridItem;
  @Input() resizeEvent: any;
  @Output() panelClick = new EventEmitter<DashboardGridItem>();
  @Output() configureClick = new EventEmitter<DashboardGridItem>();

  onConfigureClick(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.configureClick.emit(this.widget);
  }
}
