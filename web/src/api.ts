export class ApiError extends Error {
  constructor(public details: any) {
    super(details.message || "Request failed");
  }
}
export async function api<T = any>(path: string, body?: unknown): Promise<T> {
  const response = await fetch("/api/v1" + path, {
    method: body === undefined ? "GET" : "POST",
    headers: body === undefined ? {} : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await response.json();
  if (!response.ok) throw new ApiError(data);
  return data;
}
export async function connect() {
  const fragment = new URLSearchParams(location.hash.slice(1));
  const token = fragment.get("token");
  if (token) {
    const response = await fetch("/api/v1/session", {
      method: "POST",
      headers: { "X-ZKM-Token": token },
    });
    if (!response.ok) throw new Error("Unable to start session");
    history.replaceState(null, "", location.pathname + location.search);
  }
  return api("/state");
}
export function fileUrl(path: string) {
  return "/api/v1/artifact?path=" + encodeURIComponent(path);
}
