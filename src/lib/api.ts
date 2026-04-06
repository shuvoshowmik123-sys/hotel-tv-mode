export async function api(url: string, options: RequestInit = {}) {
    const isFormData = options.body instanceof FormData;

    const headers: HeadersInit = {};
    if (!isFormData) {
        headers["Content-Type"] = "application/json";
    }

    // Merge custom headers
    if (options.headers) {
        Object.assign(headers, options.headers);
    }

    const response = await fetch(url, {
        ...options,
        headers,
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new Error(data.error || "Request failed");
    }

    return data;
}
