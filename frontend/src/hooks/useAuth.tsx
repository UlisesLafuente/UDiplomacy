import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'
import { auth } from '@/api'
import type { Role } from '@/types'

interface AuthState {
  token: string | null
  username: string | null
  role: Role | null
}

interface AuthContextType extends AuthState {
  login: (username: string, password: string) => Promise<void>
  register: (username: string, password: string, role?: Role) => Promise<void>
  logout: () => void
  isAdmin: boolean
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() => ({
    token: localStorage.getItem('token'),
    username: localStorage.getItem('username'),
    role: localStorage.getItem('role') as Role | null,
  }))

  const saveAuth = useCallback((token: string, username: string, role: Role) => {
    localStorage.setItem('token', token)
    localStorage.setItem('username', username)
    localStorage.setItem('role', role)
    setState({ token, username, role })
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const res = await auth.login({ username, password })
    saveAuth(res.token, res.username, res.role)
  }, [saveAuth])

  const register = useCallback(async (username: string, password: string) => {
    const res = await auth.register({ username, password })
    saveAuth(res.token, res.username, res.role)
  }, [saveAuth])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    setState({ token: null, username: null, role: null })
  }, [])

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        register,
        logout,
        isAdmin: state.role === 'ADMIN',
        isAuthenticated: !!state.token,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
