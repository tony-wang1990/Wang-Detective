export interface ApiEnvelope<T> {
  success: boolean;
  msg?: string;
  data: T;
  code?: number;
  message?: string;
}

export interface LoginResponse {
  token: string;
  currentVersion?: string;
  latestVersion?: string;
}

export interface GlanceData {
  users?: number;
  tasks?: number;
  regions?: number;
  days?: number;
  currentVersion?: string;
}

export interface HealthData {
  status?: string;
  databaseConnectivity?: boolean;
  memoryStatus?: boolean;
  usedMemoryBytes?: number;
  maxMemoryBytes?: number;
  uptimeSeconds?: number;
  version?: string;
}

export type PageResult<T = Record<string, unknown>> = {
  records?: T[];
  total?: number;
  size?: number;
  current?: number;
  pages?: number;
};

function authHeaders(): HeadersInit {
  const token = sessionStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function parseResponse<T>(response: Response, url: string): Promise<T> {
  if (!response.ok) {
    throw new Error(`${url} ${response.status}`);
  }
  return response.json();
}

export async function apiGet<T>(url: string): Promise<ApiEnvelope<T>> {
  const response = await fetch(`/api${url}`, {
    headers: authHeaders()
  });
  return parseResponse<ApiEnvelope<T>>(response, url);
}

export async function apiPost<T>(url: string, body: unknown): Promise<ApiEnvelope<T>> {
  const response = await fetch(`/api${url}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(body)
  });
  return parseResponse<ApiEnvelope<T>>(response, url);
}

export async function opsGet<T>(url: string): Promise<ApiEnvelope<T>> {
  const response = await fetch(`/api/ops${url}`, {
    headers: authHeaders()
  });
  return parseResponse<ApiEnvelope<T>>(response, url);
}

export async function opsPost<T>(url: string, body: unknown): Promise<ApiEnvelope<T>> {
  const response = await fetch(`/api/ops${url}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(body)
  });
  return parseResponse<ApiEnvelope<T>>(response, url);
}

export async function rawGet<T>(url: string): Promise<T> {
  const response = await fetch(url, {
    headers: authHeaders()
  });
  return parseResponse<T>(response, url);
}

export async function getHealth(): Promise<HealthData> {
  const response = await fetch('/actuator/health', {
    headers: authHeaders()
  });
  if (!response.ok) {
    throw new Error(`health ${response.status}`);
  }
  return response.json();
}
