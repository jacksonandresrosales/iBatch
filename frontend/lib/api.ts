const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type AvailableFileResponse = {
  fileName: string;
  sizeBytes: number;
  lastModifiedAt: string;
  expectedFormat: boolean;
};

export type ProcessedFileResponse = {
  id: number;
  fileName: string;
  status: "PROCESANDO" | "PROCESADO" | "PROCESADO_CON_RECHAZOS" | "ERROR";
  totalTransactions: number;
  processedTransactions: number;
  rejectedTransactions: number;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};

export type TransactionRejectionResponse = {
  transactionRejectionId: number;
  reasonCode: string;
  reasonName: string;
  message: string | null;
  createdAt: string;
};

export type TransactionDetailResponse = {
  transactionId: number;
  lineNumber: number;
  rawAccount: string | null;
  rawAmount: string | null;
  rawDate: string | null;
  account: string | null;
  amount: number | null;
  transactionDate: string | null;
  status: "PROCESADO" | "RECHAZADA";
  rejections: TransactionRejectionResponse[];
  createdAt: string;
  updatedAt: string;
};

export type FileDetailResponse = {
  file: ProcessedFileResponse;
  transactions: TransactionDetailResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ReprocessTransactionResponse = {
  transactionId: number;
  fileId: number;
  status: "PROCESADO" | "RECHAZADA";
  message: string;
};

export type RejectionReasonSummaryResponse = {
  code: string;
  name: string;
  count: number;
};

export type ProcessingLogResponse = {
  id: number;
  fileId: number | null;
  transactionId: number | null;
  fileName: string | null;
  level: "INFO" | "SUCCESS" | "WARNING" | "ERROR";
  event: string;
  message: string;
  createdAt: string;
};

export type DashboardSummaryResponse = {
  totalFiles: number;
  totalProcessedTransactions: number;
  totalRejectedTransactions: number;
  rejectionRate: number;
  rejectionReasons: RejectionReasonSummaryResponse[];
  recentFiles: ProcessedFileResponse[];
  recentEvents: ProcessingLogResponse[];
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ProcessFileResponse = {
  fileId: number;
  fileName: string;
  status: "PROCESANDO";
  message: string;
  totalRecords: number;
  processedCount: number;
  rejectedCount: number;
};

export type FileProgressResponse = {
  fileId: number;
  fileName: string;
  processedCount: number;
  rejectedCount: number;
  totalRecords: number;
  percentage: number;
  status: "PROCESANDO" | "PROCESADO" | "PROCESADO_CON_RECHAZOS" | "ERROR";
  completed: boolean;
  error: boolean;
};

export type AuthenticatedUserResponse = {
  username: string;
  role: "ADMIN" | "OPERATOR";
};

type CsrfTokenResponse = {
  headerName: string;
  token: string;
};

type ErrorResponse = { message?: string };

let csrfToken: CsrfTokenResponse | null = null;

async function getCsrfToken(): Promise<CsrfTokenResponse> {
  if (csrfToken) return csrfToken;

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/auth/csrf`, {
      credentials: "include",
      cache: "no-store",
    });
  } catch {
    throw new Error("No se pudo conectar con el servidor");
  }

  if (!response.ok) {
    const error = (await response.json().catch(() => ({}))) as ErrorResponse;
    throw new Error(error.message ?? "No se pudo iniciar una sesión segura");
  }

  csrfToken = (await response.json()) as CsrfTokenResponse;
  return csrfToken;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (init?.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const method = init?.method?.toUpperCase() ?? "GET";
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrf = await getCsrfToken();
    headers.set(csrf.headerName, csrf.token);
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      credentials: "include",
      headers,
    });
  } catch {
    throw new Error("No se pudo conectar con el servidor");
  }

  if (!response.ok) {
    const error = (await response.json().catch(() => ({}))) as ErrorResponse;
    if (response.status === 403) csrfToken = null;
    if (
      response.status === 401
      && path !== "/auth/login"
      && typeof window !== "undefined"
      && window.location.pathname !== "/login"
    ) {
      csrfToken = null;
      window.location.assign("/login");
    }
    throw new Error(error.message ?? "No se pudo completar la operación");
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export async function login(username: string, password: string) {
  const user = await request<AuthenticatedUserResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  csrfToken = null;
  return user;
}

export function getAuthenticatedUser() {
  return request<AuthenticatedUserResponse>("/auth/me");
}

export async function logout() {
  await request<void>("/auth/logout", { method: "POST" });
  csrfToken = null;
}

export function getAvailableFiles() {
  return request<AvailableFileResponse[]>("/files/available");
}

export function uploadCsv(file: File) {
  const body = new FormData();
  body.append("file", file);

  return request<AvailableFileResponse>("/files/upload", {
    method: "POST",
    body,
  });
}

export function processFile(fileName: string) {
  return request<ProcessFileResponse>("/files/process", {
    method: "POST",
    body: JSON.stringify({ fileName }),
  });
}

export function getProcessedFiles() {
  return request<ProcessedFileResponse[]>("/files");
}

export function getFileDetail(fileId: number, page = 0, size = 50, status?: "PROCESADO" | "RECHAZADA", account?: string) {
  const parameters = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) parameters.set("status", status);
  if (account) parameters.set("account", account);
  return request<FileDetailResponse>(`/files/${fileId}?${parameters}`);
}

export function reprocessTransaction(transactionId: number, amount: number) {
  return request<ReprocessTransactionResponse>(`/transactions/${transactionId}`, {
    method: "POST",
    body: JSON.stringify({ amount }),
  });
}

export function getDashboardSummary() {
  return request<DashboardSummaryResponse>("/dashboard/summary");
}

export function getProcessingLogs(page = 0, size = 50) {
  return request<PageResponse<ProcessingLogResponse>>(`/dashboard/logs?page=${page}&size=${size}`);
}

export function getFileProgress(fileId: number) {
  return request<FileProgressResponse>(`/files/${fileId}/progress`);
}
