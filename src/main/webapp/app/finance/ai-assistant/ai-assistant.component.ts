import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, ElementRef, SecurityContext, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AlertError } from 'app/shared/alert/alert-error';
import { AiAssistantService } from './ai-assistant.service';
import { AiAssistantMessage, AiAssistantNavigationSuggestion } from './ai-assistant.types';

@Component({
  selector: 'jhi-ai-assistant',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, RouterLink, AlertError],
  templateUrl: './ai-assistant.component.html',
  styleUrl: './ai-assistant.component.scss',
})
export class AiAssistantComponent {
  private static readonly QUICK_PROMPTS = [
    'What changed most across my dashboard metrics over the last month?',
    'Which categories and payees stand out in my recent transactions?',
    'Summarise my current portfolio holdings and currency exposure.',
    'What should I review next based on my FX rates and portfolio mix?',
  ] as const;

  private static readonly NAVIGATION_CATALOG: AiAssistantNavigationSuggestionRule[] = [
    {
      label: 'Open Dashboard',
      route: '/finance',
      description: 'Review top-level metrics and recent changes.',
      keywords: ['dashboard', 'net worth', 'metric', 'overview', 'summary', 'change', 'trend'],
    },
    {
      label: 'Open Transactions',
      route: '/finance/transactions',
      description: 'Inspect recent activity, spending, and inflows.',
      keywords: ['transaction', 'spending', 'expense', 'income', 'merchant', 'recent activity', 'cash flow'],
    },
    {
      label: 'Open Accounts',
      route: '/finance/account-list',
      description: 'Drill into balances and account-level detail.',
      keywords: ['account', 'balance', 'bank', 'cash', 'liability'],
    },
    {
      label: 'Open Portfolio',
      route: '/finance/portfolio',
      description: 'Review holdings, sector mix, and allocation.',
      keywords: ['portfolio', 'holding', 'allocation', 'sector', 'exposure', 'shares', 'investment'],
    },
    {
      label: 'Open Investments',
      route: '/finance/investment',
      description: 'Explore detailed investment positions.',
      keywords: ['investment', 'security', 'position', 'equity', 'fund'],
    },
    {
      label: 'Open Categories',
      route: '/finance/categories',
      description: 'Review category structure and classification.',
      keywords: ['category', 'categories', 'classification', 'budget bucket'],
    },
    {
      label: 'Open Payees',
      route: '/finance/payees',
      description: 'Inspect merchants and payee relationships.',
      keywords: ['payee', 'merchant', 'vendor', 'counterparty'],
    },
    {
      label: 'Open FX Rates',
      route: '/finance/fx-rates',
      description: 'Check foreign exchange rates and currency moves.',
      keywords: ['fx', 'foreign exchange', 'currency', 'aud', 'usd', 'exchange rate'],
    },
    {
      label: 'Open Planner',
      route: '/finance/planner',
      description: 'Turn next steps into a plan.',
      keywords: ['plan', 'next step', 'forecast', 'scenario'],
    },
    {
      label: 'Open Import',
      route: '/finance/import',
      description: 'Bring in fresh data if the assistant spots gaps.',
      keywords: ['import', 'upload', 'missing data', 'sync'],
    },
  ];

  @ViewChild('messageInput') messageInput?: ElementRef<HTMLTextAreaElement>;

  readonly messages = signal<AiAssistantMessage[]>([
    {
      role: 'assistant',
      content: 'Ask me about your dashboard metrics, accounts, categories, payees, portfolio holdings, FX rates, or recent transactions.',
      navigationSuggestions: [
        {
          label: 'Open Dashboard',
          route: '/finance',
          description: 'Start from the portfolio-wide overview.',
        },
      ],
    },
  ]);
  readonly isSending = signal(false);
  readonly model = signal<string | null>(null);

  draft = '';

  private readonly assistantService = inject(AiAssistantService);
  private readonly sanitizer = inject(DomSanitizer);

  sendMessage(): void {
    const message = this.draft.trim();
    if (!message || this.isSending()) {
      return;
    }

    const history = this.messages().filter(item => item.content.trim().length > 0);
    this.messages.set([...history, { role: 'user', content: message }]);
    this.draft = '';
    this.isSending.set(true);

    this.assistantService
      .query({ message, history })
      .pipe(finalize(() => this.isSending.set(false)))
      .subscribe({
        next: response => {
          this.model.set(response.model);
          const navigationSuggestions = this.buildNavigationSuggestions(message, response.answer);
          this.messages.set([
            ...this.messages(),
            {
              role: 'assistant',
              content: response.answer,
              navigationSuggestions,
            },
          ]);
          queueMicrotask(() => this.messageInput?.nativeElement.focus());
        },
        error: error => {
          this.messages.set([
            ...this.messages(),
            {
              role: 'assistant',
              tone: 'error',
              content: this.buildErrorMessage(error),
              navigationSuggestions: [
                {
                  label: 'Open AI Settings',
                  route: '/settings/ai',
                  description: 'Check your Gemini API key and model settings.',
                },
              ],
            },
          ]);
          queueMicrotask(() => this.messageInput?.nativeElement.focus());
        },
      });
  }

  sendSuggestedQuestion(question: string): void {
    this.draft = question;
    this.sendMessage();
  }

  quickPrompts(): readonly string[] {
    return AiAssistantComponent.QUICK_PROMPTS;
  }

  handleComposerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  trackMessage(index: number): number {
    return index;
  }

  renderMessageContent(message: AiAssistantMessage): string {
    const html =
      message.role === 'assistant'
        ? this.renderMarkdown(message.content)
        : `<p>${this.escapeHtml(message.content).replace(/\n/g, '<br>')}</p>`;
    return this.sanitizer.sanitize(SecurityContext.HTML, html) ?? '';
  }

  trackSuggestion(index: number): number {
    return index;
  }

  private buildErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'The assistant request failed for an unexpected reason. Please try again.';
    }

    const detail = this.extractProblemDetail(error);
    const statusLabel = error.status > 0 ? `${error.status} ${error.statusText}`.trim() : 'Request Failed';

    return detail ? `The assistant request failed (${statusLabel}).\n${detail}` : `The assistant request failed (${statusLabel}).`;
  }

  private extractProblemDetail(error: HttpErrorResponse): string | null {
    const payload = error.error;

    if (typeof payload === 'string') {
      return payload.trim() || null;
    }

    if (payload && typeof payload === 'object') {
      const problem = payload as { detail?: string; message?: string; title?: string };
      return problem.detail?.trim() || problem.message?.trim() || problem.title?.trim() || null;
    }

    return error.message?.trim() || null;
  }

  private renderMarkdown(content: string): string {
    const escaped = this.escapeHtml(content);
    const fencedBlocks: string[] = [];

    let html = escaped.replace(/```([\w-]+)?\n([\s\S]*?)```/g, (_match, language: string | undefined, code: string) => {
      const index =
        fencedBlocks.push(
          `<pre class="ai-assistant__code-block"><code${language ? ` data-language="${language}"` : ''}>${code.trimEnd()}</code></pre>`,
        ) - 1;
      return `@@CODE_BLOCK_${index}@@`;
    });

    html = html.replace(/`([^`\n]+)`/g, '<code>$1</code>');
    html = html.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>');
    html = this.renderMarkdownBlocks(html);

    return html.replace(/@@CODE_BLOCK_(\d+)@@/g, (_match, index: string) => fencedBlocks[Number(index)] ?? '');
  }

  private renderMarkdownBlocks(content: string): string {
    const lines = content.split('\n');
    const rendered: string[] = [];
    let paragraph: string[] = [];
    let listItems: string[] = [];
    let listType: 'ul' | 'ol' | null = null;

    const flushParagraph = (): void => {
      if (paragraph.length > 0) {
        rendered.push(`<p>${paragraph.join('<br>')}</p>`);
        paragraph = [];
      }
    };

    const flushList = (): void => {
      if (listType && listItems.length > 0) {
        rendered.push(`<${listType}>${listItems.join('')}</${listType}>`);
      }
      listItems = [];
      listType = null;
    };

    for (const line of lines) {
      const trimmed = line.trim();

      if (!trimmed) {
        flushParagraph();
        flushList();
        continue;
      }

      if (/^@@CODE_BLOCK_\d+@@$/.test(trimmed)) {
        flushParagraph();
        flushList();
        rendered.push(trimmed);
        continue;
      }

      const heading = trimmed.match(/^(#{1,6})\s+(.+)$/);
      if (heading) {
        flushParagraph();
        flushList();
        const level = heading[1].length;
        rendered.push(`<h${level}>${heading[2]}</h${level}>`);
        continue;
      }

      const unorderedItem = trimmed.match(/^[-*]\s+(.+)$/);
      if (unorderedItem) {
        flushParagraph();
        if (listType !== 'ul') {
          flushList();
          listType = 'ul';
        }
        listItems.push(`<li>${unorderedItem[1]}</li>`);
        continue;
      }

      const orderedItem = trimmed.match(/^\d+\.\s+(.+)$/);
      if (orderedItem) {
        flushParagraph();
        if (listType !== 'ol') {
          flushList();
          listType = 'ol';
        }
        listItems.push(`<li>${orderedItem[1]}</li>`);
        continue;
      }

      flushList();
      paragraph.push(trimmed);
    }

    flushParagraph();
    flushList();

    return rendered.join('');
  }

  private escapeHtml(value: string): string {
    return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  private buildNavigationSuggestions(userMessage: string, assistantResponse: string): AiAssistantNavigationSuggestion[] {
    const haystack = `${userMessage}\n${assistantResponse}`.toLowerCase();
    const suggestions = AiAssistantComponent.NAVIGATION_CATALOG.filter(rule =>
      rule.keywords.some(keyword => haystack.includes(keyword.toLowerCase())),
    ).map(({ label, route, description }) => ({ label, route, description }));

    return suggestions.slice(0, 3);
  }
}

interface AiAssistantNavigationSuggestionRule extends AiAssistantNavigationSuggestion {
  keywords: string[];
}
