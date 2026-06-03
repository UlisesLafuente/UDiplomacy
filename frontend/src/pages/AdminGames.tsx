import { useEffect, useState } from 'react'
import { admin } from '@/api'
import { useAuth } from '@/hooks/useAuth'
import { Link } from 'react-router-dom'
import type { GameReference } from '@/types'

export default function AdminGames() {
  const { username, logout } = useAuth()
  const [gameList, setGameList] = useState<GameReference[]>([])

  useEffect(() => {
    admin.listGames().then(setGameList).catch(() => {})
  }, [])

  const deleteGame = async (gameId: string) => {
    if (!confirm('Delete this game permanently?')) return
    try {
      await admin.deleteGame(gameId)
      setGameList((prev) => prev.filter((g) => g.gameId !== gameId))
    } catch {
      alert('Error deleting game')
    }
  }

  return (
    <div className="mx-auto max-w-4xl p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Admin — Games</h1>
        <div className="flex items-center gap-4">
          <Link to="/admin/users" className="text-sm text-blue-600 hover:underline">Users</Link>
          <Link to="/admin/maps" className="text-sm text-blue-600 hover:underline">Maps</Link>
          <span className="text-sm text-gray-600">{username}</span>
          <button onClick={logout} className="text-sm text-red-600 hover:underline">Logout</button>
        </div>
      </div>

      <div className="space-y-3">
        {gameList.map((g) => (
          <div
            key={g.gameId}
            className="flex items-center justify-between rounded-lg border p-4"
          >
            <div>
              <p className="font-medium">{g.gameName}</p>
              <p className="text-sm text-gray-500">
                {g.username} · {g.status} · {new Date(g.createdAt).toLocaleDateString()}
              </p>
            </div>
            <button
              onClick={() => deleteGame(g.gameId)}
              className="rounded-full p-1 text-gray-400 hover:bg-red-100 hover:text-red-600"
              title="Delete game"
            >
              ✕
            </button>
          </div>
        ))}
        {gameList.length === 0 && (
          <p className="py-8 text-center text-gray-400">No games found.</p>
        )}
      </div>
    </div>
  )
}
