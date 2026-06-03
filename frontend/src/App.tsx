import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from '@/hooks/useAuth'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import Games from '@/pages/Games'
import GameDetail from '@/pages/GameDetail'
import AdminMaps from '@/pages/AdminMaps'
import AdminUsers from '@/pages/AdminUsers'
import AdminGames from '@/pages/AdminGames'
import type { ReactNode } from 'react'

function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <>{children}</>
}

function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth()
  if (!isAdmin) return <Navigate to="/games" replace />
  return <>{children}</>
}

function Redirector() {
  const { isAuthenticated, isAdmin } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (isAdmin) return <Navigate to="/admin/users" replace />
  return <Navigate to="/games" replace />
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/games"
            element={
              <RequireAuth>
                <Games />
              </RequireAuth>
            }
          />
          <Route
            path="/games/:id"
            element={
              <RequireAuth>
                <GameDetail />
              </RequireAuth>
            }
          />
          <Route
            path="/admin/games"
            element={
              <RequireAdmin>
                <AdminGames />
              </RequireAdmin>
            }
          />
          <Route
            path="/admin/users"
            element={
              <RequireAdmin>
                <AdminUsers />
              </RequireAdmin>
            }
          />
          <Route
            path="/admin/maps"
            element={
              <RequireAdmin>
                <AdminMaps />
              </RequireAdmin>
            }
          />
          <Route path="*" element={<Redirector />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
