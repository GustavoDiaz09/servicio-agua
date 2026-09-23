import { useEffect } from 'react'
import { CheckCircle, CircleNotch, Drop, X, XCircle } from '@phosphor-icons/react'

export const inputCls =
  'w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 ' +
  'placeholder:text-slate-400 focus:border-accent-500 focus:outline-none focus:ring-2 focus:ring-accent-500/30 ' +
  'dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500'

export const buttonCls = {
  primary:
    'inline-flex items-center justify-center gap-2 rounded-lg bg-accent-600 px-4 py-2 text-sm font-medium ' +
    'text-white transition-colors hover:bg-accent-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent-500/50 ' +
    'disabled:pointer-events-none disabled:opacity-60 active:scale-[0.98] ' +
    'dark:bg-accent-500 dark:text-accent-950 dark:hover:bg-accent-400',
  secondary:
    'inline-flex items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium ' +
    'text-slate-700 transition-colors hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent-500/50 ' +
    'disabled:pointer-events-none disabled:opacity-60 active:scale-[0.98] ' +
    'dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800',
  danger:
    'inline-flex items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white ' +
    'transition-colors hover:bg-red-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500/50 ' +
    'disabled:pointer-events-none disabled:opacity-60 active:scale-[0.98]',
  ghost:
    'inline-flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-slate-600 ' +
    'transition-colors hover:bg-slate-100 focus:outline-none disabled:pointer-events-none disabled:opacity-60 ' +
    'dark:text-slate-300 dark:hover:bg-slate-800',
}

export function Button({ variant = 'primary', className = '', block = false, children, ...props }) {
  return (
    <button className={`${buttonCls[variant]} ${block ? 'w-full' : ''} ${className}`} {...props}>
      {children}
    </button>
  )
}

const badgeTones = {
  neutral: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
  green: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300',
  amber: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
  red: 'bg-red-100 text-red-700 dark:bg-red-500/15 dark:text-red-300',
  sky: 'bg-accent-100 text-accent-700 dark:bg-accent-500/15 dark:text-accent-300',
  violet: 'bg-violet-100 text-violet-700 dark:bg-violet-500/15 dark:text-violet-300',
}

export function Badge({ tone = 'neutral', children }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${badgeTones[tone]}`}
    >
      {children}
    </span>
  )
}

export function estadoTone(estado) {
  const e = String(estado || '').toUpperCase()
  if (['ACTIVO', 'ACTIVA', 'PAGADA', 'RESUELTA', 'COMPLETADO', 'CORRECTIVO'].includes(e)) return 'green'
  if (['PENDIENTE', 'PARCIAL', 'ESTIMADA', 'EN_CURSO', 'PROGRAMADO', 'PREVENTIVO'].includes(e)) return 'amber'
  if (['ANULADA', 'VENCIDA', 'SUSPENDIDA', 'DADA_DE_BAJA', 'RETIRADO', 'REPORTADA', 'EN_ESPERA'].includes(e)) {
    return 'red'
  }
  return 'neutral'
}

export function Field({ label, error, hint, required = false, className = '', children }) {
  return (
    <label className={`block ${className}`}>
      {label && (
        <span className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">
          {label}
          {required && <span className="ml-0.5 text-red-500">*</span>}
        </span>
      )}
      {children}
      {hint && <span className="mt-1 block text-xs text-slate-500 dark:text-slate-400">{hint}</span>}
      {error && <span className="mt-1 block text-sm text-red-600 dark:text-red-400">{error}</span>}
    </label>
  )
}

export function Spinner({ size = 16, className = '' }) {
  return <CircleNotch size={size} className={`animate-spin ${className}`} weight="bold" />
}

export function Skeleton({ className = 'h-4' }) {
  return <div className={`animate-pulse rounded-md bg-slate-200 dark:bg-slate-800 ${className}`} />
}

export function EmptyState({ icon: Icon = Drop, title = 'Sin datos', text, children }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-14 text-center">
      <div className="rounded-2xl bg-slate-100 p-4 text-slate-400 dark:bg-slate-800 dark:text-slate-500">
        <Icon size={28} weight="duotone" />
      </div>
      <h3 className="mt-4 text-base font-semibold">{title}</h3>
      {text && <p className="mt-1 max-w-sm text-sm text-slate-500 dark:text-slate-400">{text}</p>}
      {children && <div className="mt-5">{children}</div>}
    </div>
  )
}

export function DataTable({
  columns,
  data = [],
  keyField = 'id',
  loading = false,
  onRowClick,
  emptyTitle = 'Sin resultados',
  emptyText = 'No hay registros para mostrar.',
  className = '',
}) {
  if (loading) {
    return (
      <div className={`card overflow-hidden ${className}`}>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/50">
              <tr>
                {columns.map((c) => (
                  <th key={c.key} className="px-4 py-3 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                    {c.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {[...Array(6)].map((_, i) => (
                <tr key={i} className="border-b border-slate-100 last:border-0 dark:border-slate-800">
                  {columns.map((c) => (
                    <td key={c.key} className="whitespace-nowrap px-4 py-3">
                      <Skeleton className="h-3.5 w-24" />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    )
  }

  if (data.length === 0) {
    return (
      <div className={`card ${className}`}>
        <EmptyState title={emptyTitle} text={emptyText} />
      </div>
    )
  }

  return (
    <div className={`card overflow-hidden ${className}`}>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/50">
            <tr>
              {columns.map((c) => (
                <th
                  key={c.key}
                  className={`px-4 py-3 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400 ${
                    c.align === 'right' ? 'text-right' : ''
                  }`}
                >
                  {c.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
            {data.map((row) => (
              <tr
                key={row[keyField]}
                className={onRowClick ? 'hoverable-row cursor-pointer' : 'hoverable-row'}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
              >
                {columns.map((c) => (
                  <td
                    key={c.key}
                    className={`px-4 py-3 ${c.align === 'right' ? 'text-right' : ''} ${
                      c.className || ''
                    }`}
                  >
                    {c.render ? c.render(row) : row[c.key]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>}
      </div>
      {children && <div className="flex flex-wrap items-center gap-2">{children}</div>}
    </div>
  )
}

const statTones = {
  sky: 'bg-accent-50 text-accent-700 dark:bg-accent-500/10 dark:text-accent-300',
  green: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300',
  amber: 'bg-amber-50 text-amber-700 dark:bg-amber-500/10 dark:text-amber-300',
  red: 'bg-red-50 text-red-700 dark:bg-red-500/10 dark:text-red-300',
  slate: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200',
}

export function StatCard({ label, value, icon: Icon = Drop, tone = 'sky', hint }) {
  return (
    <div className="card p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{label}</p>
          <p className="num mt-1 text-2xl font-semibold tracking-tight">{value}</p>
          {hint && <p className="mt-1 text-xs text-slate-400 dark:text-slate-500">{hint}</p>}
        </div>
        <div className={`rounded-xl p-2.5 ${statTones[tone]}`}>
          <Icon size={22} weight="duotone" />
        </div>
      </div>
    </div>
  )
}

export function Modal({ open, onClose, title, children, footer, size = 'md' }) {
  useEffect(() => {
    if (!open) return
    const onKey = (e) => {
      if (e.key === 'Escape') onClose?.()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null

  const widths = { sm: 'max-w-md', md: 'max-w-2xl', lg: 'max-w-4xl' }

  return (
    <div
      className="fixed inset-0 z-[52] flex items-end justify-center sm:items-center sm:p-6"
      role="dialog"
      aria-modal="true"
    >
      <div className="absolute inset-0 bg-slate-950/40 backdrop-blur-sm" onClick={onClose} />
      <div className={`card relative flex max-h-[92dvh] w-full ${widths[size]} flex-col`}>
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 dark:border-slate-800">
          <h2 className="text-base font-semibold">{title}</h2>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800"
            aria-label="Cerrar"
          >
            <X size={18} />
          </button>
        </div>
        <div className="overflow-y-auto px-6 py-5">{children}</div>
        {footer && (
          <div className="flex justify-end gap-3 border-t border-slate-200 px-6 py-4 dark:border-slate-800">
            {footer}
          </div>
        )}
      </div>
    </div>
  )
}

export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title = '¿Confirmar la acción?',
  message,
  confirmLabel = 'Confirmar',
  busy = false,
}) {
  return (
    <Modal
      open={open}
      onClose={busy ? undefined : onClose}
      title={title}
      size="sm"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            Cancelar
          </Button>
          <Button variant="danger" onClick={onConfirm} disabled={busy}>
            {busy ? <Spinner size={14} /> : null}
            {busy ? 'Procesando…' : confirmLabel}
          </Button>
        </>
      }
    >
      <div className="flex items-start gap-3">
        <span className="mt-0.5 rounded-full bg-red-100 p-1.5 text-red-600 dark:bg-red-500/15 dark:text-red-400">
          <XCircle size={20} weight="duotone" />
        </span>
        <p className="text-sm text-slate-600 dark:text-slate-300">{message}</p>
      </div>
    </Modal>
  )
}

export { CheckCircle }