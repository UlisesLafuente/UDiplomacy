import { useEffect, useRef, useState, useCallback, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { games } from '@/api'
import { useAuth } from '@/hooks/useAuth'
import {
  getNationColor,
  getProvinceCenter,
  getAllProvinceCenters,
  buildNationColorsFromSvg,
  renderUnit,
  addArrow,
  svgNs,
} from '@/utils/map'
import type { Game, RetreatOptionsResponse } from '@/types'

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

  // Retreat panel state
  const [retreatOptions, setRetreatOptions] = useState<RetreatOptionsResponse | null>(null)
  const [retreatSelections, setRetreatSelections] = useState<Record<string, string>>({})

  // Tooltip state
  const [tooltip, setTooltip] = useState<{ x: number; y: number; text: string } | null>(null)
  const [syntaxTooltip, setSyntaxTooltip] = useState<{ x: number; y: number } | null>(null)

  const handleSvgMouseLeave = () => setTooltip(null)

  // Zoom / Pan state
  const [zoom, setZoom] = useState(1)
  const [pan, setPan] = useState({ x: 0, y: 0 })
  const isPanningRef = useRef(false)
  const panStartRef = useRef({ x: 0, y: 0 })
  const [isPanning, setIsPanning] = useState(false)
  const mapContainerRef = useRef<HTMLDivElement>(null)

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault()
    const factor = e.deltaY > 0 ? 0.9 : 1.1
    setZoom((prev) => Math.max(0.3, Math.min(5, prev * factor)))
  }

  const handleMouseDown = (e: React.MouseEvent) => {
    if (e.button === 1) {
      e.preventDefault()
      isPanningRef.current = true
      panStartRef.current = { x: e.clientX - pan.x, y: e.clientY - pan.y }
      setIsPanning(true)
    }
  }

  const handleMouseMove = (e: React.MouseEvent) => {
    const target = e.target as Element
    const unitGroup = target.closest('[data-unit-label]') as HTMLElement | null
    if (unitGroup) {
      setTooltip({ x: e.clientX + 12, y: e.clientY - 10, text: unitGroup.getAttribute('data-unit-label')! })
    } else {
      setTooltip(null)
    }
  }

  useEffect(() => {
    const handleGlobalMouseMove = (e: MouseEvent) => {
      if (isPanningRef.current) {
        setPan({ x: e.clientX - panStartRef.current.x, y: e.clientY - panStartRef.current.y })
      }
    }
    const handleGlobalMouseUp = () => {
      isPanningRef.current = false
      setIsPanning(false)
    }
    window.addEventListener('mousemove', handleGlobalMouseMove)
    window.addEventListener('mouseup', handleGlobalMouseUp)
    return () => {
      window.removeEventListener('mousemove', handleGlobalMouseMove)
      window.removeEventListener('mouseup', handleGlobalMouseUp)
    }
  }, [])

  // Build panel state: map of nation -> array of build/disband orders
  interface BuildEntry {
    type: 'build' | 'disband'
    value: string  // "unitType province" for build, "province" for disband
  }
  const [buildSelections, setBuildSelections] = useState<Record<string, BuildEntry[]>>({})

  const fetchGame = useCallback(async () => {
    if (!id) return
    const g = await games.get(id)
    setGame(g)
    return g
  }, [id])

  const fetchRetreatOptions = useCallback(async () => {
    if (!id) return
    try {
      const opts = await games.retreatOptions(id)
      setRetreatOptions(opts)
      setRetreatSelections(
        Object.fromEntries(opts.units.map((u) => [u.province, '']))
      )
    } catch {
      setRetreatOptions(null)
    }
  }, [id])

  const renderMap = useCallback(async (g: Game) => {
    const svg = svgRef.current
    if (!svg) return

    svg.querySelectorAll('.game-overlay').forEach((el) => el.remove())

    buildNationColorsFromSvg(svg, g.provinceOwnership ?? {})

    for (const [province, nation] of Object.entries(g.provinceOwnership ?? {})) {
      if (g.provinceTypes[province] === 'SEA') continue
      const path = svg.getElementById(`provincia-${province}`)
      if (path) {
        (path as HTMLElement).style.fill = getNationColor(nation)
      }
    }

    for (const u of g.units) {
      const center = centersRef.current[u.province] || getProvinceCenter(svg, u.province)
      if (!center) continue
      await renderUnit(u, center, svg)
    }

    g.pendingOrders.forEach((o) => {
      if (!o.target) return
      const fromCenter = centersRef.current[o.source] || getProvinceCenter(svg, o.source)
      if (!fromCenter) return

      if (o.type === 'MOVE' || o.type === 'RETREAT') {
        const toCenter = centersRef.current[o.target] || getProvinceCenter(svg, o.target)
        if (!toCenter) return
        addArrow(svg, fromCenter, toCenter, '#555', false)
      } else if (o.type === 'SUPPORT' || o.type === 'CONVOY') {
        const toCenter = centersRef.current[o.target] || getProvinceCenter(svg, o.target)
        if (!toCenter) return
        if (o.auxiliary) {
          const auxCenter = centersRef.current[o.auxiliary] || getProvinceCenter(svg, o.auxiliary)
          if (auxCenter) {
            const midX = (toCenter.x + auxCenter.x) / 2
            const midY = (toCenter.y + auxCenter.y) / 2
            addArrow(svg, fromCenter, { x: midX, y: midY }, '#555', true)
            return
          }
        }
        addArrow(svg, fromCenter, toCenter, '#555', true)
      }
    })
  }, [])

  const loadMapSvg = async (mapId: string) => {
    try {
      const token = localStorage.getItem('token')
      const resp = await fetch(`/api/maps/${mapId}/svg`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      const svgText = await resp.text()
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
        }
      }
    } catch (e) {
      console.error('Failed to load map SVG:', e)
    }
  }

  useEffect(() => {
    if (!id) return
    setLoading(true)
    fetchGame().then((g) => {
      if (g) {
        return loadMapSvg(g.mapId).then(() => renderMap(g))
      }
    }).finally(() => setLoading(false))
  }, [id, fetchGame, renderMap])

  useEffect(() => {
    if (!game) return
    renderMap(game)
    if (game.phase === 'RETREAT') {
      fetchRetreatOptions()
    } else {
      setRetreatOptions(null)
      setRetreatSelections({})
    }
    if (game.phase === 'BUILD') {
      const init: Record<string, BuildEntry[]> = {}
      for (const bc of game.buildCapacities) {
        init[bc.nation] = []
      }
      setBuildSelections(init)
    } else {
      setBuildSelections({})
    }
  }, [game, renderMap, fetchRetreatOptions])

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

  const confirmRetreats = async () => {
    if (!id || !retreatOptions) return
    const orders: string[] = []
    for (const u of retreatOptions.units) {
      const target = retreatSelections[u.province]
      if (target && target !== '') {
        orders.push(`${u.type === 'ARMY' ? 'A' : 'F'} ${u.province} R ${target}`)
      }
      // empty = disband, no order needed
    }
    await games.retreat(id, orders)
    setRetreatOptions(null)
    setRetreatSelections({})
    await fetchGame()
  }

  const addBuildEntry = (nation: string) => {
    setBuildSelections((prev) => ({
      ...prev,
      [nation]: [...(prev[nation] || []), { type: 'build' as const, value: '' }],
    }))
  }

  const addDisbandEntry = (nation: string) => {
    setBuildSelections((prev) => ({
      ...prev,
      [nation]: [...(prev[nation] || []), { type: 'disband' as const, value: '' }],
    }))
  }

  const updateBuildEntry = (nation: string, index: number, entry: BuildEntry) => {
    setBuildSelections((prev) => {
      const updated = [...(prev[nation] || [])]
      updated[index] = entry
      return { ...prev, [nation]: updated }
    })
  }

  const removeBuildEntry = (nation: string, index: number) => {
    setBuildSelections((prev) => {
      const updated = [...(prev[nation] || [])]
      updated.splice(index, 1)
      return { ...prev, [nation]: updated }
    })
  }

  const confirmBuilds = async () => {
    if (!id) return
    const orders: string[] = []
    for (const [, entries] of Object.entries(buildSelections)) {
      for (const entry of entries) {
        if (!entry.value) continue
        if (entry.type === 'build') {
          const [unitType, province] = entry.value.split(' ')
          orders.push(`${unitType} ${province} B ${province}`)
        } else {
          orders.push(`A ${entry.value} D`)
        }
      }
    }
    if (orders.length === 0) return
    await games.build(id, orders)
    setBuildSelections({})
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

  const loadHistory = async () => {
    if (!id) return
    const h = await games.history(id)
    setHistory(h)
    setShowHistory(!showHistory)
  }

  const nationUnits = (nation: string) =>
    game?.units.filter((u) => u.nation === nation) ?? []

  const freeHomeProvinces = (nation: string) => {
    if (!game) return []
    const bc = game.buildCapacities.find((b) => b.nation === nation)
    if (!bc) return []
    return bc.availableProvinces.filter(
      (p) => !game!.units.some((u) => u.province === p)
    )
  }

  const freeColonialProvinces = (nation: string) => {
    if (!game) return []
    const bc = game.buildCapacities.find((b) => b.nation === nation)
    if (!bc) return []
    return bc.colonialProvinces.filter(
      (p) => !game!.units.some((u) => u.province === p)
    )
  }

  const scores = useMemo(() => {
    if (!game) return []
    const unitCount: Record<string, number> = {}
    for (const nation of game.nations) unitCount[nation] = 0
    for (const u of game.units) {
      if (unitCount[u.nation] !== undefined) unitCount[u.nation]++
    }
    return game.nations.map((n) => ({
      nation: n,
      score: game.scores[n] ?? 0,
      units: unitCount[n],
    }))
  }, [game])

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
        {/* Map + score */}
        <div className="flex flex-1 flex-col bg-gray-50">
          <div
            ref={mapContainerRef}
            className="flex-1 overflow-hidden"
            onWheel={handleWheel}
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseLeave={handleSvgMouseLeave}
            style={{ cursor: isPanning ? 'grabbing' : 'grab' }}
          >
            <div
              style={{
                transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
                transformOrigin: '0 0',
                width: '100%',
                height: '100%',
              }}
            >
              <svg
                ref={svgRef}
                className="block"
                viewBox="0 0 3044 2401"
                preserveAspectRatio="xMidYMid meet"
                style={{ minHeight: '600px', width: '100%', background: '#e8f4e8' }}
              />
            </div>
            {tooltip && (
              <div
                className="pointer-events-none fixed z-50 rounded bg-gray-800 px-2 py-1 text-xs font-mono text-white shadow-lg"
                style={{ left: tooltip.x, top: tooltip.y }}
              >
                {tooltip.text}
              </div>
            )}
          </div>
          {game && (
            <div className="flex items-center gap-6 border-t bg-white px-6 py-2 text-sm">
              {scores.map((s) => (
                <div key={s.nation} className="flex items-center gap-1.5">
                  <span className="inline-block h-3 w-3 rounded-full" style={{ backgroundColor: getNationColor(s.nation) }} />
                  <span className="font-medium">{s.nation}</span>
                  <span className="text-gray-500">{s.score}</span>
                  <span className="text-xs text-gray-400">({s.units})</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="flex w-96 flex-col border-l bg-white">
          <div className="flex-1 overflow-auto p-4">
            {game && <>
              {/* Game controls */}
              <div className="mb-4 flex flex-wrap gap-2">
                {game.phase === 'ORDERS' && (
                  <button onClick={executeOrders} className="rounded bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700">
                    Execute
                  </button>
                )}
                <button onClick={undo} className="rounded bg-gray-500 px-3 py-1.5 text-sm text-white hover:bg-gray-600">
                  Undo
                </button>
                <button onClick={advance} className="rounded bg-blue-500 px-3 py-1.5 text-sm text-white hover:bg-blue-600">
                  Advance
                </button>
                <button onClick={() => navigate('/games')} className="rounded bg-gray-500 px-3 py-1.5 text-sm text-white hover:bg-gray-600">
                  Go back
                </button>
                <span className="relative inline-flex items-center">
                  <button
                    className="rounded-full bg-gray-200 px-2 py-0.5 text-xs font-bold text-gray-600 hover:bg-gray-300"
                    onMouseEnter={(e) => {
                      const rect = (e.target as HTMLElement).getBoundingClientRect()
                      setSyntaxTooltip({ x: rect.right, y: rect.bottom + 4 })
                    }}
                    onMouseLeave={() => setSyntaxTooltip(null)}
                  >
                    ?
                  </button>
                  {syntaxTooltip && (
                    <div
                      className="pointer-events-none fixed z-50 rounded border bg-white px-3 py-2 text-xs text-gray-700 shadow-lg"
                      style={{ right: window.innerWidth - syntaxTooltip.x, top: syntaxTooltip.y }}
                    >
                      <p className="mb-1 font-semibold">Order syntax:</p>
                      {game.phase === 'ORDERS' && <>
                        <p><span className="font-mono">A LON H</span> — hold</p>
                        <p><span className="font-mono">A PAR - BUR</span> — move</p>
                        <p><span className="font-mono">A PAR S A MAR - BUR</span> — support move</p>
                        <p><span className="font-mono">A PAR S A MAR</span> — support hold</p>
                        <p><span className="font-mono">F MID C A LON - BRE</span> — convoy</p>
                      </>}
                      {game.phase === 'RETREAT' && <>
                        <p><span className="font-mono">A LON R PAR</span> — retreat</p>
                        <p><span className="font-mono">A LON D</span> — disband</p>
                      </>}
                      {game.phase === 'BUILD' && <>
                        <p><span className="font-mono">F LON B LON</span> — build</p>
                        <p><span className="font-mono">A PAR D</span> — disband</p>
                      </>}
                    </div>
                  )}
                </span>
              </div>

              {/* Order input — only show in ORDERS phase */}
              {game.phase === 'ORDERS' && (
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
              )}

              {/* Retreat panel */}
              {game.phase === 'RETREAT' && retreatOptions && (
                <div className="mb-4 rounded border border-orange-200 bg-orange-50 p-3">
                  <h3 className="mb-2 text-sm font-semibold text-orange-800">Retreats</h3>
                  {retreatOptions.units.map((u) => (
                    <div key={u.province} className="mb-2">
                      <label className="mb-1 block text-xs font-medium text-gray-700">
                        {u.type} {u.province} ({u.nation})
                      </label>
                      <select
                        value={retreatSelections[u.province] ?? ''}
                        onChange={(e) =>
                          setRetreatSelections((prev) => ({
                            ...prev,
                            [u.province]: e.target.value,
                          }))
                        }
                        className="w-full rounded border px-2 py-1 text-sm"
                      >
                        <option value="">(disband)</option>
                        {u.retreatOptions.map((opt) => (
                          <option key={opt} value={opt}>
                            {opt}
                          </option>
                        ))}
                      </select>
                    </div>
                  ))}
                  <button
                    onClick={confirmRetreats}
                    className="mt-2 rounded bg-orange-600 px-3 py-1.5 text-sm text-white hover:bg-orange-700"
                  >
                    Confirm retreats
                  </button>
                </div>
              )}

              {/* Build panel */}
              {game.phase === 'BUILD' && (
                <div className="mb-4 rounded border border-purple-200 bg-purple-50 p-3">
                  <h3 className="mb-2 text-sm font-semibold text-purple-800">
                    Builds / Disbands
                    {game.colonialRule && <span className="ml-2 text-xs font-normal text-purple-600">(colonial rule)</span>}
                  </h3>
                  {game.buildCapacities.map((bc) => {
                    const entries = buildSelections[bc.nation] ?? []
                    const units = nationUnits(bc.nation)
                    const freeHomes = freeHomeProvinces(bc.nation)
                    const freeColonial = freeColonialProvinces(bc.nation)
                    return (
                      <div key={bc.nation} className="mb-3 rounded border border-purple-100 bg-white p-2">
                        <div className="mb-1 flex items-center justify-between">
                          <span className="text-xs font-bold text-gray-700">{bc.nation}</span>
                          <span className="text-xs text-gray-500">
                            {bc.buildsAvailable > 0 && `+${bc.buildsAvailable} `}
                            {bc.colonialBuildsAvailable > 0 && `+${bc.colonialBuildsAvailable}c `}
                            {bc.disbandsRequired > 0 && `-${bc.disbandsRequired} `}
                          </span>
                        </div>
                        {entries.map((entry, ei) => (
                          <div key={ei} className="mb-1 flex items-center gap-1">
                            {entry.type === 'build' ? (
                              <select
                                value={entry.value}
                                onChange={(e) => {
                                  const val = e.target.value
                                  updateBuildEntry(bc.nation, ei, { type: 'build', value: val })
                                }}
                                className="flex-1 rounded border px-2 py-1 text-xs"
                              >
                                <option value="">(build)</option>
                                {freeHomes.map((p) => {
                                  return (
                                    <optgroup key={`home-${p}`} label={`${p} (home)`}>
                                      <option value={`A ${p}`}>A {p}</option>
                                      <option value={`F ${p}`}>F {p}</option>
                                    </optgroup>
                                  )
                                })}
                                {bc.colonialBuildsAvailable > 0 && freeColonial.map((p) => {
                                  return (
                                    <optgroup key={`col-${p}`} label={`${p} (colonial)`}>
                                      <option value={`A ${p}`}>A {p}</option>
                                      <option value={`F ${p}`}>F {p}</option>
                                    </optgroup>
                                  )
                                })}
                              </select>
                            ) : (
                              <select
                                value={entry.value}
                                onChange={(e) => {
                                  const val = e.target.value
                                  updateBuildEntry(bc.nation, ei, { type: 'disband', value: val })
                                }}
                                className="flex-1 rounded border px-2 py-1 text-xs"
                              >
                                <option value="">(disband)</option>
                                {units.map((u) => (
                                  <option key={u.province} value={u.province}>
                                    {u.type === 'ARMY' ? 'A' : 'F'} {u.province}
                                  </option>
                                ))}
                              </select>
                            )}
                            <button
                              onClick={() => removeBuildEntry(bc.nation, ei)}
                              className="text-xs text-red-500 hover:text-red-700"
                            >
                              ✕
                            </button>
                          </div>
                        ))}
                        <div className="flex gap-1">
                          {(bc.buildsAvailable > 0 || bc.colonialBuildsAvailable > 0) && (
                            <button
                              onClick={() => addBuildEntry(bc.nation)}
                              className="rounded bg-purple-100 px-2 py-0.5 text-xs text-purple-700 hover:bg-purple-200"
                            >
                              + Build
                            </button>
                          )}
                          {bc.disbandsRequired > 0 && (
                            <button
                              onClick={() => addDisbandEntry(bc.nation)}
                              className="rounded bg-red-100 px-2 py-0.5 text-xs text-red-700 hover:bg-red-200"
                            >
                              + Disband
                            </button>
                          )}
                        </div>
                      </div>
                    )
                  })}
                  <button
                    onClick={confirmBuilds}
                    className="mt-2 rounded bg-purple-600 px-3 py-1.5 text-sm text-white hover:bg-purple-700"
                  >
                    Confirm builds
                  </button>
                </div>
              )}

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
