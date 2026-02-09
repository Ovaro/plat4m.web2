import { Injectable } from '@angular/core';
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
  private suffix?: string;

  private sessionId = '';

  constructor(
    private router: Router,
    private authServerProvider: AuthServerProvider,
    private location: Location,
  ) {}

  connect(): void {
    if (this.stompClient?.connected) {
      return;
    }

    let that = this;
    // building absolute path so that websocket doesn't fail when deploying with a context path
    let url = '/websocket/import';
    url = this.location.prepareExternalUrl(url);
    // const authToken = this.authServerProvider.getToken();
    // if (authToken) {
    //   url += '?access_token=' + authToken;
    // }
    const socket: WebSocket = new SockJS(url);
    this.stompClient = Stomp.over(socket, { protocols: ['v12.stomp'] });
    const headers: ConnectionHeaders = {};
    this.stompClient.connect(headers, frame => {
      this.suffix = frame?.headers['user-name'];
      // eslint-disable-next-line no-console
      console.log('STOMP Connected: ' + this.suffix + ': ' + JSON.stringify(frame));

      this.connectionSubject.next();

      //url + "-user" + that.sessionId

      this.sessionId = this.extractSessionId(this.stompClient);

      this.stompSubscription = this.stompClient!.subscribe('/secured/user/queue/import' + '-user' + that.sessionId, (data: Message) => {
        this.listenerSubject.next(JSON.parse(data.body));
      });

      //this.sendActivity();

      // this.routerSubscription = this.router.events
      //   .pipe(filter((event: Event) => event instanceof NavigationEnd))
      //   .subscribe(() => this.sendActivity());
    });

    //this.stompClient.ws._transport.url
    // eslint-disable-next-line no-console
    // console.log('STOMP wsString: ' + JSON.stringify(this.stompClient.ws));
  }

  extractSessionId(stompClient: Client | null): string {
    if (stompClient) {
      var url = stompClient.ws._transport.url;
      console.log(stompClient.ws._transport.url);
      //ws://localhost:8080/websocket/import/817/4qj2pnbd/websocket?access_token=eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqdXN0aW4iLCJhdXRoIjoiUk9MRV9BRE1JTixST0xFX1VTRVIiLCJleHAiOjE2NzU2NjIxMTJ9.GBCCLdDzWU6jgpBBl4VnP2gsGPS3o-tVBbkcq3RO-bc1wK15euQ9fhBqBVAaStOt6oLqPYN3yYczocqm9U4Rxg
      let i = url.indexOf('/', 38);
      i++;
      let j = url.indexOf('/', i);
      let res = url.substring(i, j);
      // url = url.replace("ws://localhost:8080/spring-security-mvc-socket/secured/room/",  "");
      // url = url.replace("/websocket", "");
      // url = url.replace(/^[0-9]+\//, "");

      console.log('Your current session is: ' + res);
      return res;
    } else {
      return '';
    }
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
