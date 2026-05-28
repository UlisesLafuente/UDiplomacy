import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from '@/hooks/useAuth'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import Games from '@/pages/Games'
import GameDetail from '@/pages/GameDetail'
import AdminMaps from '@/pages/AdminMaps'
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
            path="/admin/maps"
            element={
              <RequireAdmin>
                <AdminMaps />
              </RequireAdmin>
            }
          />
          <Route path="*" element={<Navigate to="/games" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
