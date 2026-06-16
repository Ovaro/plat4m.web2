import { DOCUMENT } from '@angular/common';
import { EventEmitter, HostBinding, Injectable, inject } from '@angular/core';
import { CookieService } from 'ngx-cookie';

export type ThemePreference = 'auto' | 'light' | 'dark';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private static readonly STORAGE_KEY = 'plat4m-theme-preference';

  public onChange: EventEmitter<ThemeChangeEvent> = new EventEmitter<ThemeChangeEvent>();

  @HostBinding('style.--text-color') color = '--app-colours-primary-text-light';

  private currentTheme: ThemePreference = 'auto';
  private readonly document = inject(DOCUMENT);
  private readonly cookieService = inject(CookieService);

  constructor() {
    const savedThemePreference = this.cookieService.get(ThemeService.STORAGE_KEY);
    if (this.isThemePreference(savedThemePreference)) {
      this.currentTheme = savedThemePreference;
    }

    this.syncDocumentTheme(this.currentTheme === 'auto' ? this.getOSTheme() : this.currentTheme);

    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const ptr = this;

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function (e) {
      if (ptr.currentTheme === 'auto') {
        ptr.sendThemeEvent(ptr.getOSTheme(), 'auto');
      }
    });
    // window.matchMedia('(prefers-color-scheme: light)').addEventListener('change',
    //     function(e) {
    //         if(ptr.currentTheme === 'auto'){
    //             ptr.sendThemeEvent( 'light');
    //         }
    // });
  }

  switchTheme(theme: string): void {
    const themeLink = this.document.getElementById('app-theme') as HTMLLinkElement;
    themeLink.href = theme + '.css';
  }

  getOSTheme(): string {
    // if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    //     return 'dark';
    // }

    if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }

    return 'light';
  }

  theme(): string {
    const resolvedTheme = this.currentTheme === 'auto' ? this.getOSTheme() : this.currentTheme;
    this.syncDocumentTheme(resolvedTheme);
    return resolvedTheme;
  }

  preference(): ThemePreference {
    return this.currentTheme;
  }

  setThemePreference(themePreference: ThemePreference): void {
    this.currentTheme = themePreference;
    this.cookieService.put(ThemeService.STORAGE_KEY, themePreference);
    this.sendThemeEvent(this.currentTheme === 'auto' ? this.getOSTheme() : this.currentTheme, this.currentTheme);
  }

  toggleTheme(): void {
    const nextTheme = this.theme() === 'light' ? 'dark' : 'light';
    this.setThemePreference(nextTheme);
  }

  public sendThemeEvent(theme: string, preference: ThemePreference = this.currentTheme): void {
    this.syncDocumentTheme(theme);
    this.onChange.emit(new ThemeChangeEvent(theme, preference));
  }

  private isThemePreference(value: string | null | undefined): value is ThemePreference {
    return value === 'auto' || value === 'light' || value === 'dark';
  }

  private syncDocumentTheme(theme: string): void {
    const normalizedTheme = theme === 'auto' ? this.getOSTheme() : theme;
    this.document.body.classList.remove('theme-light', 'theme-dark');
    this.document.documentElement.classList.remove('theme-light', 'theme-dark');
    this.document.body.classList.add(`theme-${normalizedTheme}`);
    this.document.documentElement.classList.add(`theme-${normalizedTheme}`);
    this.document.body.setAttribute('data-plat4m-theme', normalizedTheme);
    this.document.documentElement.setAttribute('data-plat4m-theme', normalizedTheme);
  }

  // public updateCSSVariablesViaDom(sanitizer: DomSanitizer): void {
  //     //--app-colours-primary-text-'+[theme]+')
  //     sanitizer.bypassSecurityTrustStyle('--text-color: --app-colours-primary-text-"+this.theme()+")');
  // }
}

export class ThemeChangeEvent {
  theme: string;
  preference: ThemePreference;

  constructor(theme: string, preference: ThemePreference = 'auto') {
    this.theme = theme;
    this.preference = preference;
  }
}
