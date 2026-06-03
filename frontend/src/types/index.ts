export type ProvinceType = 'INLAND' | 'COASTAL' | 'SEA'
export type UnitType = 'ARMY' | 'FLEET'
export type OrderType = 'HOLD' | 'MOVE' | 'SUPPORT' | 'CONVOY' | 'RETREAT' | 'BUILD' | 'DISBAND'
export type OrderResult = 'SUCCESS' | 'FAILURE'
export type GameStatus = 'IN_PROGRESS' | 'FINISHED'
export type Phase = 'ORDERS' | 'RETREAT' | 'BUILD'
export type Season = 'SPRING' | 'AUTUMN'
export type Role = 'PLAYER' | 'ADMIN'

export interface Nation {
  name: string
}

export interface Territory {
  province: string
  coast: string | null
}

export interface Unit {
  type: UnitType
  nation: string
  province: string
  coast: string | null
}

export interface Order {
  unitType: UnitType
  source: string
  type: string
  target: string | null
  auxiliary: string | null
}

export interface Turn {
  year: number
  season: Season
  phase: Phase
  orders: Order[]
  results: OrderResult[]
}

export interface Game {
  gameId: string
  mapName: string
  state: string
  season: string
  year: number
  phase: string
  nations: string[]
  units: Unit[]
  pendingOrders: Order[]
  lastResolvedOrders: Order[]
  lastResolvedResults: string[]
  history: HistoryEntry[]
  winner: string | null
  dislodgedUnits: Unit[]
  buildCapacities: BuildCapacity[]
  provinceOwnership: Record<string, string>
  scores: Record<string, number>
}

export interface HistoryEntry {
  season: string
  year: number
  phase: string
  units: Unit[]
  orders: Order[]
  results: string[]
}

export interface BuildCapacity {
  nation: string
  buildsAvailable: number
  disbandsRequired: number
  availableProvinces: string[]
}

export interface RetreatOptionsResponse {
  units: DislodgedUnitOptions[]
}

export interface DislodgedUnitOptions {
  type: string
  nation: string
  province: string
  retreatOptions: string[]
}

export interface GameReference {
  gameId: string
  userId: string
  username: string
  gameName: string
  status: string
  createdAt: string
}

export interface MapVariant {
  id: string
  name: string
  svgContent: string | null
  createdAt: string
}

export interface AuthResponse {
  token: string
  username: string
  role: Role
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
}

export interface CreateGameRequest {
  mapId?: string
  mapJson?: string
}

export interface SubmitOrderRequest {
  rawOrder: string
}

export interface UserResponse {
  userId: string
  username: string
  role: Role
}


