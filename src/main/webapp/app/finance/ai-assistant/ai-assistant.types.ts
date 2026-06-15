export type AiAssistantRole = 'user' | 'assistant';
export type AiAssistantMessageTone = 'default' | 'error';

export interface AiAssistantNavigationSuggestion {
  label: string;
  route: string;
  description: string;
}

export interface AiAssistantMessage {
  role: AiAssistantRole;
  content: string;
  tone?: AiAssistantMessageTone;
  navigationSuggestions?: AiAssistantNavigationSuggestion[];
}

export interface AiAssistantRequest {
  message: string;
  history: AiAssistantMessage[];
}

export interface AiAssistantResponse {
  answer: string;
  model: string | null;
  aiConfigured: boolean;
  contextSources: string[];
}
