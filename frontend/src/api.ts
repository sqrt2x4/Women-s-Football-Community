export interface Community {
  id: string;
  name: string;
  slug: string;
  description: string;
  createdAt: string;
  memberCount: number;
}

export interface Post {
  id: string;
  communityId: string;
  authorName: string;
  content: string;
  createdAt: string;
}

export interface Comment {
  id: string;
  authorName: string;
  content: string;
  createdAt: string;
}

export interface User {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

let csrfToken: string | null = null;

async function ensureCsrfToken() {
  if (csrfToken) return csrfToken;
  const response = await fetch("/api/auth/csrf");
  if (!response.ok) throw new ApiError("Could not start a secure session", response.status);
  const result = await response.json() as { token: string };
  csrfToken = result.token;
  return csrfToken;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = (init?.method ?? "GET").toUpperCase();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...init?.headers as Record<string, string>,
  };
  if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
    headers["X-CSRF-TOKEN"] = await ensureCsrfToken();
  }
  const response = await fetch(path, {
    ...init,
    headers,
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(body?.detail ?? body?.message ?? `Request failed (${response.status})`, response.status);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  csrf: ensureCsrfToken,
  me: () => request<User>("/api/auth/me"),
  register: async (payload: { email: string; displayName: string; password: string }) => {
    const user = await request<User>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    csrfToken = null;
    await ensureCsrfToken();
    return user;
  },
  login: async (payload: { email: string; password: string }) => {
    const user = await request<User>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    csrfToken = null;
    await ensureCsrfToken();
    return user;
  },
  logout: async () => {
    await request<void>("/api/auth/logout", { method: "POST" });
    csrfToken = null;
    await ensureCsrfToken();
  },
  communities: () => request<Community[]>("/api/communities"),
  myMemberships: () => request<string[]>("/api/communities/memberships/mine"),
  createCommunity: (payload: { name: string; slug: string; description: string }) =>
    request<Community>("/api/communities", { method: "POST", body: JSON.stringify(payload) }),
  join: (id: string) =>
    request<void>(`/api/communities/${id}/memberships`, { method: "POST" }),
  leave: (id: string) =>
    request<void>(`/api/communities/${id}/memberships`, { method: "DELETE" }),
  posts: (communityId: string) => request<Post[]>(`/api/communities/${communityId}/posts`),
  createPost: (communityId: string, content: string) =>
    request<Post>(`/api/communities/${communityId}/posts`, {
      method: "POST",
      body: JSON.stringify({ content }),
    }),
  comments: (postId: string) => request<Comment[]>(`/api/posts/${postId}/comments`),
  createComment: (postId: string, content: string) =>
    request<Comment>(`/api/posts/${postId}/comments`, {
      method: "POST",
      body: JSON.stringify({ content }),
    }),
};
