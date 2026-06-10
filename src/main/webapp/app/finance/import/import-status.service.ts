import { Injectable, NgZone } from '@angular/core';
import { Location } from '@angular/common';
import { Router, NavigationEnd, Event } from '@angular/router';
import { Subscription, ReplaySubject, Subject } from 'rxjs';
import { filter } from 'rxjs/operators';
import SockJS from 'sockjs-client';
import Stomp, { Client, Subscription as StompSubscription, ConnectionHeaders, Message } from 'webstomp-client';

import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { ImportStatus } from './import-status.model';

@Injectable({ providedIn: 'root' })
export class ImportStatusService {
  private stompClient: Client | null = null;
  private routerSubscription: Subscription | null = null;
  private connectionSubject: ReplaySubject<void> = new ReplaySubject(1);
  private connectionSubscription: Subscription | null = null;
  private stompSubscription: StompSubscription | null = null;
  private listenerSubject: Subject<ImportStatus> = new Subject();

  constructor(
    private router: Router,
    private authServerProvider: AuthServerProvider,
    private location: Location,
    private ngZone: NgZone,
  ) {}

  connect(): void {
    if (this.stompClient?.connected) {
      console.log('STOMP import already connected');
      return;
    }

    // building absolute path so that websocket doesn't fail when deploying with a context path
    let url = '/websocket/import';
    url = this.location.prepareExternalUrl(url);
    const authToken = this.authServerProvider.getToken();
    if (authToken) {
      url += '?access_token=' + authToken;
    }
    console.log('STOMP import connecting to: ' + url);
    const socket: WebSocket = new SockJS(url);
    socket.onopen = () => console.log('SockJS import open');
    socket.onclose = event => console.log('SockJS import close: ' + JSON.stringify(event));
    this.stompClient = Stomp.over(socket, { protocols: ['v12.stomp'] });
    this.stompClient.debug = (message: string) => console.log('STOMP import debug: ' + message);
    const headers: ConnectionHeaders = {};
    this.stompClient.connect(
      headers,
      frame => {
        // eslint-disable-next-line no-console
        console.log('STOMP Connected: ' + JSON.stringify(frame));

        this.connectionSubject.next();

        console.log('STOMP subscribing to /secured/user/queue/import');
        this.stompSubscription = this.stompClient!.subscribe('/secured/user/queue/import', (data: Message) => {
          console.log('STOMP import message received: ' + data.body);
          this.ngZone.run(() => {
            this.listenerSubject.next(JSON.parse(data.body));
          });
        });

        //this.sendActivity();

        // this.routerSubscription = this.router.events
        //   .pipe(filter((event: Event) => event instanceof NavigationEnd))
        //   .subscribe(() => this.sendActivity());
      },
      error => {
        console.log('STOMP import connection error: ' + JSON.stringify(error));
      },
    );

    //this.stompClient.ws._transport.url
    // eslint-disable-next-line no-console
    // console.log('STOMP wsString: ' + JSON.stringify(this.stompClient.ws));
  }

  disconnect(): void {
    this.unsubscribe();

    this.connectionSubject = new ReplaySubject(1);

    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
      this.routerSubscription = null;
    }

    if (this.stompClient) {
      if (this.stompClient.connected) {
        console.log('STOMP import disconnecting active client');
        this.stompClient.disconnect();
      }
      this.stompClient = null;
      // eslint-disable-next-line no-console
      console.log('STOMP Disconnect');
    }
  }

  receive(): Subject<ImportStatus> {
    // eslint-disable-next-line no-console
    console.log('STOMP receive');
    return this.listenerSubject;
  }

  // subscribe(): void {

  //   if (this.connectionSubscription) {
  //     return;
  //   }

  //   this.connectionSubscription = this.connectionSubject.subscribe(() => {
  //     if (this.stompClient) {
  //       // eslint-disable-next-line no-console
  //       console.log('STOMP subscribe');
  //       this.stompSubscription = this.stompClient.subscribe('/topic/import', (data: Message) => {
  //         this.listenerSubject.next(JSON.parse(data.body));
  //       });
  //     }
  //   });
  // }

  unsubscribe(): void {
    if (this.stompSubscription) {
      console.log('STOMP unsubscribing from import queue');
      this.stompSubscription.unsubscribe();
      this.stompSubscription = null;
    }

    if (this.connectionSubscription) {
      this.connectionSubscription.unsubscribe();
      this.connectionSubscription = null;
    }

    // eslint-disable-next-line no-console
    console.log('STOMP unsubscribe');
  }

  private sendActivity(): void {
    if (this.stompClient?.connected) {
      this.stompClient.send(
        '/topic/import', // destination
        JSON.stringify({ page: this.router.routerState.snapshot.url }), // body
        {}, // header
      );
    }
  }
}
