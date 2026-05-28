const SVG_NS = 'http://www.w3.org/2000/svg'

export const svgNs = SVG_NS

export const NATION_COLORS: Record<string, string> = {
  ENGLAND: '#e67e22',
  FRANCE: '#3498db',
  GERMANY: '#2c3e50',
  ITALY: '#27ae60',
  RUSSIA: '#f1c40f',
  TURKEY: '#e74c3c',
  AUSTRIA: '#9b59b6',
}

const FLAGS: Record<string, string> = {
  ENGLAND: '\u{1F3F4}\u{E0067}\u{E0062}\u{E0065}\u{E006E}\u{E0067}\u{E007F}',
  FRANCE: '\u{1F1EB}\u{1F1F7}',
  GERMANY: '\u{1F1E9}\u{1F1EA}',
  ITALY: '\u{1F1EE}\u{1F1F9}',
  RUSSIA: '\u{1F1F7}\u{1F1FA}',
  TURKEY: '\u{1F1F9}\u{1F1F7}',
  AUSTRIA: '\u{1F1E6}\u{1F1F9}',
}

function getNationFlag(nation: string): string | undefined {
  return FLAGS[nation]
}

export function getNationColor(nation: string): string {
  return NATION_COLORS[nation] || nationColor(nation)
}

function nationColor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  const hue = Math.abs(hash) % 360
  return `hsl(${hue}, 65%, 50%)`
}

function getBBox(el: Element): DOMRect | null {
  try {
    return (el as SVGGraphicsElement).getBBox()
  } catch {
    return null
  }
}

export function getProvinceCenter(svg: SVGSVGElement, code: string) {
  const path = svg.getElementById(`provincia-${code}`)
  if (!path) return null
  const bbox = getBBox(path)
  if (!bbox) return null
  return { x: bbox.x + bbox.width / 2, y: bbox.y + bbox.height / 2 }
}

export function getAllProvinceCenters(svg: SVGSVGElement): Record<string, { x: number; y: number }> {
  const centers: Record<string, { x: number; y: number }> = {}
  const paths = svg.querySelectorAll('[data-code]')
  paths.forEach((path) => {
    const code = path.getAttribute('data-code')
    if (!code) return
    const bbox = getBBox(path)
    if (!bbox) return
    centers[code] = { x: bbox.x + bbox.width / 2, y: bbox.y + bbox.height / 2 }
  })
  return centers
}

const svgCache = new Map<string, string>()

async function getUnitSvg(type: string): Promise<string> {
  if (!svgCache.has(type)) {
    const resp = await fetch(`/assets/units/${type}.svg`)
    svgCache.set(type, await resp.text())
  }
  return svgCache.get(type)!
}

export async function renderUnit(
  unit: { type: string; nation: string; province: string },
  center: { x: number; y: number },
  containerSvg: SVGSVGElement,
) {
  const type = unit.type === 'ARMY' ? 'army' : 'fleet'
  const color = getNationColor(unit.nation)
  const flag = getNationFlag(unit.nation)

  const svgText = await getUnitSvg(type)
  const doc = new DOMParser().parseFromString(svgText, 'image/svg+xml')
  const unitSvg = doc.documentElement
  unitSvg.setAttribute('style', `color: ${color}`)
  unitSvg.setAttribute('width', '50')
  unitSvg.setAttribute('height', '50')
  unitSvg.setAttribute('viewBox', '0 0 50 50')

  const g = document.createElementNS(SVG_NS, 'g')
  g.setAttribute('transform', `translate(${center.x - 25}, ${center.y - 25})`)
  g.appendChild(unitSvg)

  if (flag) {
    const t = document.createElementNS(SVG_NS, 'text')
    t.setAttribute('x', '25')
    t.setAttribute('y', '16')
    t.setAttribute('text-anchor', 'middle')
    t.setAttribute('font-size', '12')
    t.textContent = flag
    g.appendChild(t)
  }

  containerSvg.appendChild(g)
}

export function addArrow(
  svg: SVGSVGElement,
  from: { x: number; y: number },
  to: { x: number; y: number },
  color = '#555',
  dashed = false,
) {
  const path = document.createElementNS(SVG_NS, 'path')
  const mx = (from.x + to.x) / 2
  const my = (from.y + to.y) / 2 - 10
  path.setAttribute('d', `M ${from.x} ${from.y} Q ${mx} ${my} ${to.x} ${to.y}`)
  path.setAttribute('stroke', color)
  path.setAttribute('stroke-width', '2.5')
  path.setAttribute('fill', 'none')
  if (dashed) path.setAttribute('stroke-dasharray', '8,5')
  const markerId = color === '#e74c3c' ? 'red' : color === '#27ae60' ? 'green' : 'gray'
  path.setAttribute('marker-end', `url(#arrowhead-${markerId})`)
  svg.appendChild(path)
}

export function addFailureCross(
  svg: SVGSVGElement,
  from: { x: number; y: number },
  to: { x: number; y: number },
) {
  const mx = (from.x + to.x) / 2
  const my = (from.y + to.y) / 2 - 20
  const text = document.createElementNS(SVG_NS, 'text')
  text.setAttribute('x', String(mx))
  text.setAttribute('y', String(my))
  text.setAttribute('fill', '#e74c3c')
  text.setAttribute('font-size', '28')
  text.setAttribute('font-weight', 'bold')
  text.setAttribute('text-anchor', 'middle')
  text.textContent = '\u2715'
  svg.appendChild(text)
}
