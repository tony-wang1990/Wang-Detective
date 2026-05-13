export interface ApiEnvelope<T> {
  success: boolean;
  msg?: string;
  data: T;
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

function authHeaders(): HeadersInit {
  const token = sessionStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function apiGet<T>(url: string): Promise<ApiEnvelope<T>> {
  const response = await fetch(`/api${url}`, {
    headers: authHeaders()
  });
  if (!response.ok) {
    throw new Error(`${url} ${response.status}`);
  }
  return response.json();
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
  if (!response.ok) {
    throw new Error(`${url} ${response.status}`);
  }
  return response.json();
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
