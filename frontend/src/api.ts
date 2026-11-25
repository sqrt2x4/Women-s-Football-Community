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

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.detail ?? body?.message ?? `Request failed (${response.status})`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  communities: () => request<Community[]>("/api/communities"),
  createCommunity: (payload: { name: string; slug: string; description: string }) =>
    request<Community>("/api/communities", { method: "POST", body: JSON.stringify(payload) }),
  join: (id: string, displayName: string) =>
    request<void>(`/api/communities/${id}/memberships`, {
      method: "POST",
      body: JSON.stringify({ displayName }),
    }),
  leave: (id: string, displayName: string) =>
    request<void>(`/api/communities/${id}/memberships?displayName=${encodeURIComponent(displayName)}`, {
      method: "DELETE",
    }),
  posts: (communityId: string) => request<Post[]>(`/api/communities/${communityId}/posts`),
  createPost: (communityId: string, authorName: string, content: string) =>
    request<Post>(`/api/communities/${communityId}/posts`, {
      method: "POST",
      body: JSON.stringify({ authorName, content }),
    }),
  comments: (postId: string) => request<Comment[]>(`/api/posts/${postId}/comments`),
  createComment: (postId: string, authorName: string, content: string) =>
    request<Comment>(`/api/posts/${postId}/comments`, {
      method: "POST",
      body: JSON.stringify({ authorName, content }),
    }),
};
