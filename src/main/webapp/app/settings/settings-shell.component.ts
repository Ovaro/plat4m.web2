import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

interface SettingsNavItem {
  label: string;
  route: string;
}

interface SettingsNavSection {
  label: string;
  items: SettingsNavItem[];
}

@Component({
  selector: 'jhi-settings-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, RouterOutlet, FontAwesomeModule],
  templateUrl: './settings-shell.component.html',
  styleUrl: './settings-shell.component.scss',
})
export class SettingsShellComponent {
  readonly filterText = signal('');
  readonly sections: SettingsNavSection[] = [
    {
      label: 'Workspace',
      items: [
        { label: 'General', route: '/settings/general' },
        { label: 'AI', route: '/settings/ai' },
        { label: 'API Keys', route: '/settings/api-keys' },
      ],
    },
  ];

  readonly filteredSections = computed(() => {
    const query = this.filterText().trim().toLowerCase();
    if (!query) {
      return this.sections;
    }

    return this.sections
      .map(section => ({
        ...section,
        items: section.items.filter(item => item.label.toLowerCase().includes(query)),
      }))
      .filter(section => section.items.length > 0);
  });
}
