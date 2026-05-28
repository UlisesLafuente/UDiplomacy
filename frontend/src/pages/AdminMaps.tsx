import { useEffect, useState } from 'react'
import { maps } from '@/api'
import type { MapVariant } from '@/types'

export default function AdminMaps() {
  const [variantList, setVariantList] = useState<MapVariant[]>([])
  const [name, setName] = useState('')
  const [mapJson, setMapJson] = useState('')
  const [svgContent, setSvgContent] = useState('')
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    maps.list().then(setVariantList).catch(() => {})
  }, [])

  const createVariant = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setCreating(true)
    try {
      await maps.create({ name, mapJson, svgContent })
      setName('')
      setMapJson('')
      setSvgContent('')
      const updated = await maps.list()
      setVariantList(updated)
    } catch (err: any) {
      setError(err.response?.data?.detail || err.message || 'Error creating variant')
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl p-6">
      <h1 className="mb-6 text-2xl font-bold">Admin — Map Variants</h1>

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

        <textarea
          className="mb-3 w-full rounded border px-3 py-2 font-mono text-xs"
          rows={6}
          placeholder="Map JSON"
          value={mapJson}
          onChange={(e) => setMapJson(e.target.value)}
          required
        />

        <textarea
          className="mb-3 w-full rounded border px-3 py-2 font-mono text-xs"
          rows={6}
          placeholder="SVG content"
          value={svgContent}
          onChange={(e) => setSvgContent(e.target.value)}
          required
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
          <div key={v.id} className="rounded-lg border p-4">
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
        ))}
      </div>
    </div>
  )
}
