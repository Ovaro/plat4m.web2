import { Component, OnInit, RendererFactory2, Renderer2 } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router, ActivatedRouteSnapshot, NavigationEnd, RouterModule } from '@angular/router';
import { TranslateService, LangChangeEvent } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';
import { ThemeChangeEvent, ThemeService } from './theme.service';
import { DomSanitizer } from '@angular/platform-browser';

import { AccountService } from 'app/core/auth/account.service';
import { CookieModule, CookieService } from 'ngx-cookie';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { SidenavComponent } from '../sidenav/sidenav.component';
import { Alert as AlertComponent } from 'app/shared/alert/alert';

@Component({
  selector: 'jhi-main',
  standalone: true,
  templateUrl: './main.html',
  imports: [CommonModule, RouterModule, FontAwesomeModule, SidenavComponent, CookieModule, AlertComponent],
  styleUrls: ['./main.component.scss'],
})
export default class MainComponent implements OnInit {
  static COOKIE_SIDENAV_STATE = 'sidenav-state';

  theme = 'light';
  navType: string;
  expanded = true;
  private renderer: Renderer2;

  constructor(
    private accountService: AccountService,
    private titleService: Title,
    private router: Router,
    private translateService: TranslateService,
    private themeService: ThemeService,
    private cookieService: CookieService,
    private sanitizer: DomSanitizer,
    rootRenderer: RendererFactory2,
  ) {
    this.renderer = rootRenderer.createRenderer(document.querySelector('html'), null);
    this.navType = 'side';
    this.theme = this.themeService.theme();
  }

  ngOnInit(): void {
    // try to log in automatically
    this.accountService.identity().subscribe();

    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;
      //this.themeService.updateCSSVariablesViaDom(this.sanitizer);
    });

    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.updateTitle();
      }
    });

    this.translateService.onLangChange.subscribe((langChangeEvent: LangChangeEvent) => {
      this.updateTitle();
      dayjs.locale(langChangeEvent.lang);
      this.renderer.setAttribute(document.querySelector('html'), 'lang', langChangeEvent.lang);
    });

    this.accountService.identity().subscribe(account => {
      if (account) {
        if (!account.navType) {
          this.navType = 'side'; // account.navType
        } else {
          this.navType = account.navType;
        }
      }
    });

    const navbarState = this.getCookie(MainComponent.COOKIE_SIDENAV_STATE);

    if (navbarState !== undefined) {
      this.expanded = navbarState.toLowerCase() === 'true';
    }
  }

  getCookie(key: string): any {
    return this.cookieService.get(key);
  }

  saveCookie(key: string, value: any): void {
    return this.cookieService.put(key, value);
  }

  toggleExpand(): void {
    this.expanded = !this.expanded;
    this.saveCookie(MainComponent.COOKIE_SIDENAV_STATE, this.expanded);
    // eslint-disable-next-line no-console
    //console.log('Saved Sidenav State to Cookie: ', this.COOKIE_SIDENAV_STATE, this.getCookie(this.COOKIE_SIDENAV_STATE));
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  isAuthenticated(): boolean {
    //// eslint-disable-next-line no-console
    // console.log('Is authenticated: {}',this.accountService.isAuthenticated());
    // this.accountService.identity().subscribe(account => {
    //   if (account) {
    //      if(!account.navType) {
    //        this.navType = 'side'; // account.navType
    //      } else {
    //       this.navType = account.navType;
    //     }
    //     this.userActivated = account.activated;
    //   }
    // });

    return this.accountService.isAuthenticated();
  }

  isVerified(): boolean {
    return !this.accountService.isAuthenticated() || Boolean(this.accountService.account()?.activated);
  }

  private getPageTitle(routeSnapshot: ActivatedRouteSnapshot): string {
    const title: string = routeSnapshot.data['pageTitle'] ?? '';
    if (routeSnapshot.firstChild) {
      return this.getPageTitle(routeSnapshot.firstChild) || title;
    }
    return title;
  }

  private updateTitle(): void {
    let pageTitle = this.getPageTitle(this.router.routerState.snapshot.root);
    if (!pageTitle) {
      pageTitle = 'global.title';
    }
    this.translateService.get(pageTitle).subscribe(title => this.titleService.setTitle(title));
  }
}
