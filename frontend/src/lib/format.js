const moneyFmt = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 2,
  minimumFractionDigits: 0,
})

const numberFmt = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 })

export function formatMoney(value) {
  if (value === null || value === undefined) return '—'
  return moneyFmt.format(value)
}

export function formatNumber(value) {
  if (value === null || value === undefined) return '—'
  return numberFmt.format(value)
}

export function formatDate(value) {
  if (!value) return '—'
  return new Date(value + 'T00:00:00').toLocaleDateString('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export function m3(value) {
  if (value === null || value === undefined) return '—'
  return `${formatNumber(value)} m³`
}