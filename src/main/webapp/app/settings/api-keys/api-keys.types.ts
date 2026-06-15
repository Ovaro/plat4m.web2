export interface UserApiKey {
  id: string;
  name: string;
  createdDate: string;
  expiresAt: string;
}

export interface CreateUserApiKeyRequest {
  name: string;
  expiresAt: string;
}

export interface CreatedUserApiKeyResponse {
  apiKey: UserApiKey;
  token: string;
}
