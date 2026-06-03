import { useEffect, useRef, useState } from 'react'
import { maps, admin } from '@/api'
import { useAuth } from '@/hooks/useAuth'
import { Link } from 'react-router-dom'
import type { MapVariant } from '@/types'

export default function AdminMaps() {
  const { username, logout } = useAuth()
  const [variantList, setVariantList] = useState<MapVariant[]>([])
  const [name, setName] = useState('')
  const [mapJsonFile, setMapJsonFile] = useState<File | null>(null)
  const [svgFile, setSvgFile] = useState<File | null>(null)
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const mapJsonRef = useRef<HTMLInputElement>(null)
  const svgRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    maps.list().then(setVariantList).catch(() => {})
  }, [])

  const createVariant = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!mapJsonFile) return
    setError('')
    setCreating(true)
    try {
      const formData = new FormData()
      formData.append('name', name)
      formData.append('mapJson', mapJsonFile)
      if (svgFile) formData.append('svgContent', svgFile)
      await maps.create(formData)
      setName('')
      setMapJsonFile(null)
      setSvgFile(null)
      if (mapJsonRef.current) mapJsonRef.current.value = ''
      if (svgRef.current) svgRef.current.value = ''
      const updated = await maps.list()
      setVariantList(updated)
    } catch (err: any) {
      setError(err.response?.data?.detail || err.message || 'Error creating variant')
    } finally {
      setCreating(false)
    }
  }

  const deleteVariant = async (id: string) => {
    if (!confirm('Delete this map variant permanently?')) return
    try {
      await admin.deleteMap(id)
      setVariantList((prev) => prev.filter((v) => v.id !== id))
    } catch {
      alert('Error deleting map variant')
    }
  }

  return (
    <div className="mx-auto max-w-4xl p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Admin — Map Variants</h1>
        <div className="flex items-center gap-4">
          <Link to="/admin/games" className="text-sm text-blue-600 hover:underline">Games</Link>
          <Link to="/admin/users" className="text-sm text-blue-600 hover:underline">Users</Link>
          <span className="text-sm text-gray-600">{username}</span>
          <button onClick={logout} className="text-sm text-red-600 hover:underline">Logout</button>
        </div>
      </div>

      <form onSubmit={createVariant} className="mb-8 rounded-lg border p-4">
        <h2 className="mb-3 font-semibold">Create variant</h2>
        {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

        <input
          className="mb-3 w-full rounded border px-3 py-2"
          placeholder="Variant name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />

        <label className="mb-1 block text-sm font-medium text-gray-700">Map JSON file</label>
        <input
          ref={mapJsonRef}
          type="file"
          accept=".json,application/json"
          className="mb-3 w-full rounded border px-3 py-2 text-sm"
          onChange={(e) => setMapJsonFile(e.target.files?.[0] ?? null)}
          required
        />

        <label className="mb-1 block text-sm font-medium text-gray-700">SVG file (optional)</label>
        <input
          ref={svgRef}
          type="file"
          accept=".svg,image/svg+xml"
          className="mb-3 w-full rounded border px-3 py-2 text-sm"
          onChange={(e) => setSvgFile(e.target.files?.[0] ?? null)}
        />

        <button
          type="submit"
          disabled={creating}
          className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {creating ? 'Creating...' : 'Create'}
        </button>
      </form>

      <div className="space-y-3">
        {variantList.map((v) => (
          <div key={v.id} className="flex items-center justify-between rounded-lg border p-4">
            <div>
              <p className="font-medium">{v.name}</p>
              <p className="text-sm text-gray-500">{v.id} · {new Date(v.createdAt).toLocaleDateString()}</p>
              {v.svgContent && (
                <button
                  onClick={() => {
                    const win = window.open()
                    win?.document.write(v.svgContent!)
                  }}
                  className="mt-2 text-sm text-blue-600 hover:underline"
                >
                  Preview SVG
                </button>
              )}
            </div>
            {v.id !== 'europe-classic' && (
              <button
                onClick={() => deleteVariant(v.id)}
                className="rounded p-1 text-gray-400 hover:bg-red-100 hover:text-red-600"
                title="Delete variant"
              >
                ✕
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
