export interface AiSettings {
  provider: string;
  defaultModel: string;
  hasApiKey: boolean;
  maskedApiKey: string | null;
  modelOverrides: Record<string, string>;
}

export interface AiSettingsUpdateRequest {
  defaultModel: string;
  apiKey: string | null;
  clearApiKey: boolean;
  modelOverrides: Record<string, string>;
}
