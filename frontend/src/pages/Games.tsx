import { useEffect, useState } from 'react'
import { games, maps } from '@/api'
import { useAuth } from '@/hooks/useAuth'
import { useNavigate } from 'react-router-dom'
import type { GameReference, MapVariant } from '@/types'

export default function Games() {
  const { username, logout } = useAuth()
  const navigate = useNavigate()
  const [gameList, setGameList] = useState<GameReference[]>([])
  const [variants, setVariants] = useState<MapVariant[]>([])
  const [selectedMapId, setSelectedMapId] = useState('')
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    games.list().then(setGameList).catch(() => {})
    maps.list().then(setVariants).catch(() => {})
  }, [])

  const createGame = async () => {
    setCreating(true)
    try {
      const game = await games.create({ mapId: selectedMapId || undefined })
      navigate(`/games/${game.gameId}`)
    } catch {
      setCreating(false)
    }
  }

  const deleteGame = async (gameId: string, e: React.MouseEvent) => {
    e.stopPropagation()
    if (!confirm('¿Eliminar esta partida permanentemente?')) return
    try {
      await games.delete(gameId)
      setGameList((prev) => prev.filter((g) => g.gameId !== gameId))
    } catch {
      alert('Error al eliminar la partida')
    }
  }

  return (
    <div className="mx-auto max-w-4xl p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Partidas</h1>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">{username}</span>
          <button onClick={logout} className="text-sm text-red-600 hover:underline">
            Logout
          </button>
        </div>
      </div>

      <div className="mb-8 rounded-lg border p-4">
        <h2 className="mb-3 font-semibold">Nueva partida</h2>
        <div className="flex gap-3">
          <select
            className="flex-1 rounded border px-3 py-2"
            value={selectedMapId}
            onChange={(e) => setSelectedMapId(e.target.value)}
          >
            <option value="">— Seleccionar mapa —</option>
            {variants.map((v) => (
              <option key={v.id} value={v.id}>
                {v.name}
              </option>
            ))}
          </select>
          <button
            onClick={createGame}
            disabled={creating || !selectedMapId}
            className="rounded bg-green-600 px-4 py-2 text-white hover:bg-green-700 disabled:opacity-50"
          >
            {creating ? 'Creando...' : 'Crear'}
          </button>
        </div>
      </div>

      <div className="space-y-3">
        {gameList.map((g) => (
          <div
            key={g.gameId}
            className="flex cursor-pointer items-center justify-between rounded-lg border p-4 hover:bg-gray-50"
            onClick={() => navigate(`/games/${g.gameId}`)}
          >
            <div>
              <p className="font-medium">{g.gameName}</p>
              <p className="text-sm text-gray-500">
                {g.status} · {new Date(g.createdAt).toLocaleDateString()}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <span
                className={`rounded-full px-3 py-1 text-xs font-medium ${
                  g.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600'
                }`}
              >
                {g.status === 'IN_PROGRESS' ? 'En curso' : 'Finalizada'}
              </span>
              <button
                onClick={(e) => deleteGame(g.gameId, e)}
                className="rounded-full p-1 text-gray-400 hover:bg-red-100 hover:text-red-600"
                title="Eliminar partida"
              >
                ✕
              </button>
            </div>
          </div>
        ))}
        {gameList.length === 0 && (
          <p className="py-8 text-center text-gray-400">No hay partidas. ¡Crea una!</p>
        )}
      </div>
    </div>
  )
}
