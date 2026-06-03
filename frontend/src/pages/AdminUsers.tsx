import { useEffect, useState } from 'react'
import { admin } from '@/api'
import { useAuth } from '@/hooks/useAuth'
import { Link } from 'react-router-dom'
import type { UserResponse, Role } from '@/types'

export default function AdminUsers() {
  const { username, logout } = useAuth()
  const [users, setUsers] = useState<UserResponse[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    admin.listUsers().then(setUsers).catch(() => setError('Failed to load users'))
  }, [])

  const handleRoleChange = async (userId: string, role: Role) => {
    try {
      await admin.updateRole(userId, role)
      setUsers((prev) =>
        prev.map((u) => (u.userId === userId ? { ...u, role } : u)),
      )
    } catch {
      setError('Failed to update role')
    }
  }

  const handleDeleteUser = async (userId: string) => {
    if (!confirm('Delete this user permanently?')) return
    try {
      await admin.deleteUser(userId)
      setUsers((prev) => prev.filter((u) => u.userId !== userId))
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to delete user')
    }
  }

  return (
    <div className="mx-auto max-w-4xl p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Admin — Users</h1>
        <div className="flex items-center gap-4">
          <Link to="/admin/games" className="text-sm text-blue-600 hover:underline">Games</Link>
          <Link to="/admin/maps" className="text-sm text-blue-600 hover:underline">Maps</Link>
          <span className="text-sm text-gray-600">{username}</span>
          <button onClick={logout} className="text-sm text-red-600 hover:underline">Logout</button>
        </div>
      </div>

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-left text-sm">
          <thead className="border-b bg-gray-50">
            <tr>
              <th className="px-4 py-3 font-medium">Username</th>
              <th className="px-4 py-3 font-medium">Role</th>
              <th className="px-4 py-3 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.userId} className="border-b last:border-0 hover:bg-gray-50">
                <td className="px-4 py-3">{u.username}</td>
                <td className="px-4 py-3">
                  <select
                    className="rounded border px-2 py-1 text-xs"
                    value={u.role}
                    onChange={(e) => handleRoleChange(u.userId, e.target.value as Role)}
                  >
                    <option value="PLAYER">PLAYER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td className="px-4 py-3">
                  <button
                    onClick={() => handleDeleteUser(u.userId)}
                    disabled={u.username === username}
                    className="rounded px-2 py-1 text-xs text-red-600 hover:bg-red-50 disabled:opacity-30"
                    title={u.username === username ? 'Cannot delete yourself' : 'Delete user'}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {users.length === 0 && (
          <p className="py-8 text-center text-gray-400">No users found.</p>
        )}
      </div>
    </div>
  )
}
