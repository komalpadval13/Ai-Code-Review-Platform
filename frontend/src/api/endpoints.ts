import api from './client';
import type { ApiResponse, AuthResponse, Project, Submission, DashboardStats, PageResponse } from '../types';

export const authApi = {
  register: (data: { username: string; email: string; password: string; fullName: string }) =>
    api.post<ApiResponse<AuthResponse>>('/auth/register', data),
  login: (data: { usernameOrEmail: string; password: string }) =>
    api.post<ApiResponse<AuthResponse>>('/auth/login', data),
  refresh: (refreshToken: string) =>
    api.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken }),
  logout: () => api.post('/auth/logout'),
  validate: () => api.get<ApiResponse<{ valid: boolean; user: AuthResponse['user'] }>>('/auth/validate'),
};

export const projectApi = {
  list: (page = 0, size = 20, search?: string) =>
    api.get<ApiResponse<PageResponse<Project>>>('/projects', { params: { page, size, search } }),
  get: (id: number) => api.get<ApiResponse<Project>>(`/projects/${id}`),
  create: (data: { name: string; description?: string; language?: string; isPublic?: boolean; tags?: string }) =>
    api.post<ApiResponse<Project>>('/projects', data),
  update: (id: number, data: Partial<Project>) =>
    api.put<ApiResponse<Project>>(`/projects/${id}`, data),
  delete: (id: number) => api.delete(`/projects/${id}`),
};

export const submissionApi = {
  upload: (projectId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<ApiResponse<Submission>>(`/submissions/upload/${projectId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  get: (id: number) => api.get<ApiResponse<Submission>>(`/submissions/${id}`),
  listByProject: (projectId: number, page = 0) =>
    api.get<ApiResponse<PageResponse<Submission>>>(`/submissions/project/${projectId}`, { params: { page } }),
  listMy: (page = 0) =>
    api.get<ApiResponse<PageResponse<Submission>>>('/submissions/my', { params: { page } }),
  getSource: (id: number) => api.get<ApiResponse<string>>(`/submissions/${id}/source`),
  downloadReport: (id: number) =>
    api.get(`/submissions/${id}/report`, { responseType: 'blob' }),
};

export const analyticsApi = {
  dashboard: () => api.get<ApiResponse<DashboardStats>>('/analytics/dashboard'),
};

export const adminApi = {
  stats: () => api.get<ApiResponse<any>>('/admin/stats'),
  users: (page = 0, search?: string) =>
    api.get<ApiResponse<PageResponse<any>>>('/admin/users', { params: { page, size: 20, search } }),
  toggleUser: (id: number) => api.put(`/admin/users/${id}/toggle`),
  auditLogs: (page = 0) =>
    api.get<ApiResponse<PageResponse<any>>>('/admin/audit-logs', { params: { page, size: 50 } }),
};
