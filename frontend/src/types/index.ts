export type ProvinceType = 'INLAND' | 'COASTAL' | 'SEA'
export type UnitType = 'ARMY' | 'FLEET'
export type OrderType = 'HOLD' | 'MOVE' | 'SUPPORT' | 'CONVOY' | 'RETREAT' | 'BUILD' | 'DISBAND'
export type OrderResult = 'SUCCESS' | 'FAILURE'
export type GameState = 'IN_PROGRESS' | 'FINISHED'
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
  orderType: OrderType
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
  id: string
  mapName: string
  state: GameState
  currentYear: number
  currentSeason: Season
  currentPhase: Phase
  nations: string[]
  units: Unit[]
  pendingOrders: Order[]
  lastResolvedOrders: Order[]
  lastResolvedResults: OrderResult[]
  history: Turn[]
  winner: string | null
  dislodgedUnits: Unit[]
  buildCapacities: BuildCapacity[]
}

export interface BuildCapacity {
  nation: string
  buildsAvailable: number
  disbandsRequired: number
}

export interface GameReference {
  gameId: string
  name: string
  state: GameState
  createdAt: string
  updatedAt: string
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
  role?: Role
}

export interface CreateGameRequest {
  mapId?: string
  mapJson?: string
}

export interface SubmitOrderRequest {
  rawOrder: string
}

export interface CreateMapVariantRequest {
  name: string
  mapJson: string
  svgContent: string
}
