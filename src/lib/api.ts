"use client";

export type ApiErrorPayload = {
    error?: string;
    message?: string;
    details?: string;
    fieldErrors?: Record<string, string>;
};

export class ApiError extends Error {
    status: number;
    fieldErrors: Record<string, string>;
    isUnauthorized: boolean;

    constructor(message: string, status: number, fieldErrors: Record<string, string> = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.fieldErrors = fieldErrors;
        this.isUnauthorized = status === 401;
    }
}

function normalizeErrorMessage(status: number, data: ApiErrorPayload) {
    const message = data.error || data.message || data.details;
    if (message) {
        return message;
    }
    if (status === 401) {
        return "Your session has expired. Please sign in again.";
    }
    if (status === 403) {
        return "You do not have permission to perform this action.";
    }
    if (status >= 500) {
        return "The server could not complete this request right now.";
    }
    return "Request failed.";
}

export async function api<T = any>(url: string, options: RequestInit = {}) {
    const isFormData = options.body instanceof FormData;
    const headers: HeadersInit = {};
    if (!isFormData) {
        headers["Content-Type"] = "application/json";
    }
    if (options.headers) {
        Object.assign(headers, options.headers);
    }

    let response: Response;
    try {
        response = await fetch(url, {
            ...options,
            headers,
        });
    } catch {
        throw new ApiError("Unable to reach the admin server. Please check your connection and try again.", 0);
    }

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new ApiError(
            normalizeErrorMessage(response.status, data),
            response.status,
            typeof data?.fieldErrors === "object" && data.fieldErrors ? data.fieldErrors : {}
        );
    }

    return data as T;
}
