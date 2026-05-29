import api from './client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  Game,
  GameReference,
  CreateGameRequest,
  SubmitOrderRequest,
  MapVariant,
  CreateMapVariantRequest,
  RetreatOptionsResponse,
} from '@/types'

export const auth = {
  login: (data: LoginRequest) =>
    api.post<AuthResponse>('/auth/login', data).then((r) => r.data),
  register: (data: RegisterRequest) =>
    api.post<AuthResponse>('/auth/register', data).then((r) => r.data),
}

export const games = {
  list: () =>
    api.get<GameReference[]>('/games').then((r) => r.data),
  get: (id: string) =>
    api.get<Game>(`/games/${id}`).then((r) => r.data),
  create: (data: CreateGameRequest) =>
    api.post<Game>('/games', data).then((r) => r.data),
  submitOrder: (gameId: string, rawOrder: string) =>
    api.post(`/games/${gameId}/orders`, { rawOrder } as SubmitOrderRequest),
  removeOrder: (gameId: string, index: number) =>
    api.delete(`/games/${gameId}/orders/${index}`),
  execute: (gameId: string) =>
    api.post(`/games/${gameId}/execute`),
  retreat: (gameId: string, orders: string[]) =>
    api.post(`/games/${gameId}/retreats`, orders.map((o) => ({ rawOrder: o }))),
  retreatOptions: (gameId: string) =>
    api.get<RetreatOptionsResponse>(`/games/${gameId}/retreat-options`).then((r) => r.data),
  build: (gameId: string, orders: string[]) =>
    api.post(`/games/${gameId}/builds`, orders.map((o) => ({ rawOrder: o }))),
  undo: (gameId: string) =>
    api.post(`/games/${gameId}/undo`),
  advance: (gameId: string) =>
    api.post(`/games/${gameId}/advance`),
  rewind: (gameId: string, turnIndex: number) =>
    api.post(`/games/${gameId}/rewind/${turnIndex}`),
  history: (gameId: string) =>
    api.get<Game>(`/games/${gameId}/history`).then((r) => r.data),
  delete: (gameId: string) =>
    api.delete(`/games/${gameId}`),
}

export const maps = {
  list: () =>
    api.get<MapVariant[]>('/maps').then((r) => r.data),
  get: (id: string) =>
    api.get<MapVariant>(`/maps/${id}`).then((r) => r.data),
  getSvg: (id: string) =>
    api.get<string>(`/maps/${id}/svg`, { responseType: 'text' }).then((r) => r.data),
  create: (data: CreateMapVariantRequest) =>
    api.post<MapVariant>('/admin/maps', data).then((r) => r.data),
}

export const orders = {
  syntax: () =>
    api.get<string>('/orders/syntax').then((r) => r.data),
}
