import { EventEmitter, Injectable, inject } from '@angular/core';
import { CookieService } from 'ngx-cookie';
import { colorSchemeDark, colorSchemeLight, themeAlpine, themeBalham, themeMaterial, themeQuartz, type Theme } from 'ag-grid-community';

export type AgGridThemeName = 'alpine' | 'balham' | 'material' | 'quartz';

export interface AgGridThemeOption {
  label: string;
  value: AgGridThemeName;
}

export const AG_GRID_THEME_OPTIONS: AgGridThemeOption[] = [
  { label: 'Alpine', value: 'alpine' },
  { label: 'Balham', value: 'balham' },
  { label: 'Material', value: 'material' },
  { label: 'Quartz', value: 'quartz' },
];

@Injectable({ providedIn: 'root' })
export class AgGridThemeService {
  private static readonly STORAGE_KEY = 'plat4m-ag-grid-theme';
  private static readonly DEFAULT_THEME: AgGridThemeName = 'alpine';

  readonly onChange = new EventEmitter<AgGridThemeName>();
  private readonly cookieService = inject(CookieService);

  theme(): AgGridThemeName {
    const storedTheme = this.cookieService.get(AgGridThemeService.STORAGE_KEY);
    return this.isValidTheme(storedTheme) ? storedTheme : AgGridThemeService.DEFAULT_THEME;
  }

  setTheme(theme: AgGridThemeName): void {
    this.cookieService.put(AgGridThemeService.STORAGE_KEY, theme);
    this.onChange.emit(theme);
  }

  getThemeClass(theme: AgGridThemeName, colorMode: string): string {
    return colorMode === 'dark' ? `ag-theme-${theme}-dark` : `ag-theme-${theme}`;
  }

  getThemeDefinition(theme: AgGridThemeName, colorMode: string = 'light'): Theme {
    const ibmPlexSans = [{ googleFont: 'IBM Plex Sans' }, 'IBM Plex Sans', 'sans-serif'];
    const colorScheme = colorMode === 'dark' ? colorSchemeDark : colorSchemeLight;

    switch (theme) {
      case 'balham':
        return themeBalham.withPart(colorScheme).withParams({ fontFamily: ibmPlexSans });
      case 'material':
        return themeMaterial.withPart(colorScheme).withParams({ fontFamily: ibmPlexSans });
      case 'quartz':
        return themeQuartz.withPart(colorScheme).withParams({ fontFamily: ibmPlexSans });
      case 'alpine':
      default:
        return themeAlpine.withPart(colorScheme).withParams({ fontFamily: ibmPlexSans });
    }
  }

  private isValidTheme(theme: string | null | undefined): theme is AgGridThemeName {
    return AG_GRID_THEME_OPTIONS.some(option => option.value === theme);
  }
}
