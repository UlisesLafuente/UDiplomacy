import { useEffect, useRef, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { games } from '@/api'
import { useAuth } from '@/hooks/useAuth'
import {
  getNationColor,
  getProvinceCenter,
  getAllProvinceCenters,
  renderUnit,
  svgNs,
} from '@/utils/map'
import type { Game } from '@/types'

export default function GameDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [game, setGame] = useState<Game | null>(null)
  const [, setLoading] = useState(true)
  const [orderText, setOrderText] = useState('')
  const [history, setHistory] = useState<Game | null>(null)
  const [showHistory, setShowHistory] = useState(false)
  const svgRef = useRef<SVGSVGElement>(null)
  const centersRef = useRef<Record<string, { x: number; y: number }>>({})

  const fetchGame = useCallback(async () => {
    if (!id) return
    const g = await games.get(id)
    setGame(g)
    return g
  }, [id])

  const renderMap = useCallback(async (g: Game) => {
    const svg = svgRef.current
    if (!svg) return

    svg.querySelectorAll('.game-overlay').forEach((el) => el.remove())

    g.units.forEach((u) => {
      const path = svg.getElementById(`provincia-${u.province}`)
      if (path) {
        (path as HTMLElement).style.fill = getNationColor(u.nation)
      }
    })

    for (const u of g.units) {
      const center = centersRef.current[u.province] || getProvinceCenter(svg, u.province)
      if (!center) continue
      await renderUnit(u, center, svg)
    }
  }, [])

  const loadMapSvg = async () => {
    try {
      const token = localStorage.getItem('token')
      const resp = await fetch('/api/maps/europe-classic/svg', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const svgText = await resp.text()
      console.log('SVG loaded, length:', svgText.length)
      if (svgRef.current && svgRef.current.parentNode) {
        const doc = new DOMParser().parseFromString(svgText, 'image/svg+xml')
        const parseErr = doc.querySelector('parsererror')
        if (parseErr) {
          console.error('Parse error:', parseErr.textContent)
          return
        }
        const loadedSvg = doc.querySelector('svg')
        if (loadedSvg) {
          const parent = svgRef.current.parentNode
          const newSvg = doc.importNode(loadedSvg, true) as SVGSVGElement
          newSvg.setAttribute('class', 'h-full w-full block')
          newSvg.setAttribute('preserveAspectRatio', 'xMidYMid meet')
          newSvg.setAttribute('style', 'min-height:600px;background:#e8f4e8')
          parent.replaceChild(newSvg, svgRef.current)
          svgRef.current = newSvg
          ensureArrowDefs(newSvg)
          centersRef.current = getAllProvinceCenters(newSvg)
          console.log('SVG rendered, centers:', Object.keys(centersRef.current).length)
        } else {
          console.error('No svg element found in parsed document')
        }
      }
    } catch (e) {
      console.error('Failed to load map SVG:', e)
    }
  }

  useEffect(() => {
    if (!id) return
    setLoading(true)
    Promise.all([fetchGame(), loadMapSvg()]).then(([g]) => {
      if (g) renderMap(g)
    }).finally(() => setLoading(false))
  }, [id, fetchGame, renderMap])

  useEffect(() => {
    if (!game) return
    renderMap(game)
  }, [game, renderMap])

  const submitOrder = async () => {
    if (!id || !orderText.trim()) return
    try {
      await games.submitOrder(id, orderText.trim())
      setOrderText('')
      await fetchGame()
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Error submitting order')
    }
  }

  const removeOrder = async (index: number) => {
    if (!id) return
    await games.removeOrder(id, index)
    await fetchGame()
  }

  const executeOrders = async () => {
    if (!id) return
    await games.execute(id)
    await fetchGame()
  }

  const handleRetreat = async () => {
    if (!id) return
    const orders = prompt('Retreat orders (comma separated):')
    if (!orders) return
    await games.retreat(id, orders.split(',').map((o) => o.trim()))
    await fetchGame()
  }

  const handleBuild = async () => {
    if (!id) return
    const orders = prompt('Build/disband orders (comma separated):')
    if (!orders) return
    await games.build(id, orders.split(',').map((o) => o.trim()))
    await fetchGame()
  }

  const undo = async () => {
    if (!id) return
    await games.undo(id)
    await fetchGame()
  }

  const advance = async () => {
    if (!id) return
    await games.advance(id)
    await fetchGame()
  }

  const deleteGame = async () => {
    if (!id || !confirm('Delete this game permanently?')) return
    await games.delete(id)
    navigate('/games')
  }

  const loadHistory = async () => {
    if (!id) return
    const h = await games.history(id)
    setHistory(h)
    setShowHistory(!showHistory)
  }

  return (
    <div className="flex h-screen flex-col">
      {game && <>
        {/* Header */}
        <header className="flex items-center justify-between border-b bg-white px-6 py-3">
          <div className="flex items-center gap-4">
            <h1 className="text-lg font-bold">{game.mapName}</h1>
            <span className="rounded bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
              {game.season} {game.year} · {game.phase}
            </span>
            {game.winner && (
              <span className="rounded bg-yellow-100 px-2 py-0.5 text-xs font-medium text-yellow-700">
                Winner: {game.winner}
              </span>
            )}
          </div>
          <div className="flex items-center gap-3">
            <button onClick={() => navigate('/games')} className="text-sm text-gray-600 hover:underline">
              Games
            </button>
            <button onClick={logout} className="text-sm text-red-600 hover:underline">
              Logout
            </button>
          </div>
        </header>
      </>}

      <div className="flex flex-1 overflow-hidden">
        {/* Map */}
        <div className="flex-1 overflow-auto bg-gray-50">
          <svg
            ref={svgRef}
            className="h-full w-full block"
            viewBox="0 0 3044 2401"
            preserveAspectRatio="xMidYMid meet"
            style={{ minHeight: '600px', background: '#e8f4e8' }}
          />
        </div>

        {/* Sidebar */}
        <div className="flex w-96 flex-col border-l bg-white">
          <div className="flex-1 overflow-auto p-4">
            {game && <>
              {/* Game controls */}
              <div className="mb-4 flex flex-wrap gap-2">
                <button onClick={executeOrders} className="rounded bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700">
                  Execute
                </button>
                <button onClick={handleRetreat} className="rounded bg-orange-500 px-3 py-1.5 text-sm text-white hover:bg-orange-600">
                  Retreats
                </button>
                <button onClick={handleBuild} className="rounded bg-purple-600 px-3 py-1.5 text-sm text-white hover:bg-purple-700">
                  Builds
                </button>
                <button onClick={undo} className="rounded bg-gray-500 px-3 py-1.5 text-sm text-white hover:bg-gray-600">
                  Undo
                </button>
                <button onClick={advance} className="rounded bg-blue-500 px-3 py-1.5 text-sm text-white hover:bg-blue-600">
                  Advance
                </button>
                <button onClick={deleteGame} className="rounded bg-red-600 px-3 py-1.5 text-sm text-white hover:bg-red-700">
                  Delete
                </button>
              </div>

              {/* Order input */}
              <div className="mb-4">
                <label className="mb-1 block text-sm font-medium">New order</label>
                <div className="flex gap-2">
                  <input
                    value={orderText}
                    onChange={(e) => setOrderText(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && submitOrder()}
                    placeholder="A LON H"
                    className="flex-1 rounded border px-3 py-1.5 text-sm"
                  />
                  <button onClick={submitOrder} className="rounded bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700">
                    Send
                  </button>
                </div>
              </div>

              {/* Pending orders */}
              <div className="mb-4">
                <h3 className="mb-2 text-sm font-semibold text-gray-700">
                  Pending orders ({game.pendingOrders.length})
                </h3>
                <div className="space-y-1">
                  {game.pendingOrders.map((o, i) => (
                    <div key={i} className="flex items-center justify-between rounded bg-gray-50 px-2 py-1 text-sm">
                      <span>
                        {o.unitType} {o.source} {o.type}
                        {o.target ? ` - ${o.target}` : ''}
                      </span>
                      <button onClick={() => removeOrder(i)} className="text-xs text-red-500 hover:text-red-700">
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Resolved orders */}
              {game.lastResolvedOrders.length > 0 && (
                <div className="mb-4">
                  <h3 className="mb-2 text-sm font-semibold text-gray-700">Last resolved</h3>
                  <div className="space-y-1">
                    {game.lastResolvedOrders.map((o, i) => (
                      <div
                        key={i}
                        className={`flex items-center gap-2 rounded px-2 py-1 text-sm ${
                          game.lastResolvedResults[i] === 'SUCCESS'
                            ? 'bg-green-50 text-green-800'
                            : 'bg-red-50 text-red-800'
                        }`}
                      >
                        <span className="font-mono text-xs">
                          {o.unitType} {o.source} {o.type}
                          {o.target ? ` - ${o.target}` : ''}
                        </span>
                        <span className="ml-auto text-xs font-bold">
                          {game.lastResolvedResults[i] === 'SUCCESS' ? '✓' : '✕'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Units */}
              <div className="mb-4">
                <h3 className="mb-2 text-sm font-semibold text-gray-700">
                  Units ({game.units.length})
                </h3>
                <div className="grid grid-cols-2 gap-1">
                  {game.units.map((u, i) => (
                    <div key={i} className="flex items-center gap-1 rounded bg-gray-50 px-2 py-1 text-xs">
                      <span className="font-medium">{u.type === 'ARMY' ? 'A' : 'F'}</span>
                      <span>{u.province}</span>
                      <span className="ml-auto text-gray-500">{u.nation}</span>
                    </div>
                  ))}
                </div>
              </div>
            </>}

            {/* History toggle */}
            <button
              onClick={loadHistory}
              className="mb-4 w-full rounded border px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50"
            >
              {showHistory ? 'Hide history' : 'Show history'}
            </button>

            {showHistory && history && (
              <div className="space-y-3">
                <h3 className="text-sm font-semibold text-gray-700">History</h3>
                {history.history.map((turn, ti) => (
                  <div key={ti} className="rounded border p-2">
                    <p className="mb-1 text-xs font-bold text-gray-600">
                      {turn.season} {turn.year} · {turn.phase}
                    </p>
                    {turn.orders.map((o, oi) => (
                      <div
                        key={oi}
                        className={`flex items-center gap-1 text-xs ${
                          turn.results[oi] === 'SUCCESS' ? 'text-green-700' : 'text-red-700'
                        }`}
                      >
                        <span>
                          {o.unitType} {o.source} {o.type}
                          {o.target ? ` - ${o.target}` : ''}
                        </span>
                        <span className="ml-auto">{turn.results[oi] === 'SUCCESS' ? '✓' : '✕'}</span>
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

function ensureArrowDefs(svg: SVGSVGElement) {
  let defs = svg.querySelector('defs')
  if (!defs) {
    defs = svg.insertBefore(document.createElementNS(svgNs, 'defs'), svg.firstChild)
  }

  const markers = [
    { id: 'arrowhead-green', color: '#27ae60' },
    { id: 'arrowhead-red', color: '#e74c3c' },
    { id: 'arrowhead-gray', color: '#555' },
  ]

  for (const m of markers) {
    if (defs.querySelector(`#${m.id}`)) continue
    const marker = document.createElementNS(svgNs, 'marker')
    marker.setAttribute('id', m.id)
    marker.setAttribute('markerWidth', '12')
    marker.setAttribute('markerHeight', '8')
    marker.setAttribute('refX', '10')
    marker.setAttribute('refY', '4')
    marker.setAttribute('orient', 'auto')
    const polygon = document.createElementNS(svgNs, 'polygon')
    polygon.setAttribute('points', '0 0, 12 4, 0 8')
    polygon.setAttribute('fill', m.color)
    marker.appendChild(polygon)
    defs.appendChild(marker)
  }
}
