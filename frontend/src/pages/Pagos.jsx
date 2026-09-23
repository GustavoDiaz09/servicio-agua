import { useCallback, useEffect, useState } from 'react'
import { Coins, Plus, Receipt, WarningCircle } from '@phosphor-icons/react'
import { api } from '../api/client'
import { formatDate, formatMoney, m3 } from '../lib/format'
import {
  Badge,
  Button,
  EmptyState,
  Field,
  Modal,
  PageHeader,
  Spinner,
  StatCard,
  estadoTone,
  inputCls,
} from '../components/ui'

const ESTADOS_COBRABLE = ['PENDIENTE', 'PARCIAL', 'VENCIDA']

const ETIQUETA_ESTADO = {
  PENDIENTE: 'Pendiente',
  PARCIAL: 'Parcial',
  VENCIDA: 'Vencida',
}

const ETIQUETA_METODO = { EFECTIVO: 'Efectivo', TRANSFERENCIA: 'Transferencia', TARJETA: 'Tarjeta', QR: 'QR' }

async function facturasCobrables() {
  const grupos = await Promise.all(ESTADOS_COBRABLE.map((e) => api.get('/facturas/estado/' + e)))
  return grupos
    .flat()
    .sort((a, b) => (a.vencimiento < b.vencimiento ? -1 : 1))
}

export default function Pagos() {
  const [facturas, setFacturas] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [target, setTarget] = useState(null)
  const [pagos, setPagos] = useState([])
  const [pagosLoading, setPagosLoading] = useState(false)

  const [form, setForm] = useState({ monto: '', metodo: 'EFECTIVO', referencia: '' })
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const cargarFacturas = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setFacturas(await facturasCobrables())
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    cargarFacturas()
  }, [cargarFacturas])

  useEffect(() => {
    if (!success) return
    const t = setTimeout(() => setSuccess(''), 4000)
    return () => clearTimeout(t)
  }, [success])

  async function abrirRegistro(f) {
    setTarget(f)
    setForm({ monto: '', metodo: 'EFECTIVO', referencia: '' })
    setFormError('')
    setPagos([])
    setPagosLoading(true)
    try {
      setPagos(await api.get(`/pagos/factura/${f.id}`) || [])
    } catch {
      setPagos([])
    } finally {
      setPagosLoading(false)
    }
  }

  const totalPagado = pagos.reduce((s, p) => s + (Number(p.monto) || 0), 0)
  const deudaPendiente = target ? Math.max(0, Number(target.total) - totalPagado) : 0
  const totalDeuda = facturas.reduce(
    (s, f) => s + Math.max(0, Number(f.total)),
    0,
  )

  async function registrar(e) {
    e.preventDefault()
    const monto = Number(form.monto)
    if (!form.monto || Number.isNaN(monto) || monto <= 0) {
      setFormError('Ingresá un monto válido')
      return
    }
    if (monto > deudaPendiente) {
      setFormError(`El monto supera la deuda pendiente (${formatMoney(deudaPendiente)})`)
      return
    }
    setSaving(true)
    setFormError('')
    try {
      await api.post('/pagos', {
        facturaId: Number(target.id),
        monto,
        metodo: form.metodo,
        referencia: form.referencia.trim() || null,
      })
      setSuccess('Pago registrado correctamente')
      setTarget(null)
      await cargarFacturas()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const columns = [
    {
      key: 'numero',
      header: 'Número',
      render: (f) => <span className="font-medium text-slate-900 dark:text-slate-100">{f.numero}</span>,
    },
    { key: 'clienteNombre', header: 'Cliente' },
    {
      key: 'total',
      header: 'Total',
      align: 'right',
      render: (f) => <span className="num">{formatMoney(f.total)}</span>,
    },
    { key: 'vencimiento', header: 'Vencimiento', render: (f) => formatDate(f.vencimiento) },
    {
      key: 'estado',
      header: 'Estado',
      render: (f) => <Badge tone={estadoTone(f.estado)}>{ETIQUETA_ESTADO[f.estado] || f.estado}</Badge>,
    },
    {
      key: 'acciones',
      header: '',
      align: 'right',
      render: (f) => (
        <Button variant="ghost" className="!px-2.5" onClick={() => abrirRegistro(f)}>
          <Plus size={16} weight="bold" />
          Registrar pago
        </Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader title="Pagos" subtitle="Registro de pagos totales o parciales sobre facturas emitidas">
        <p className="num text-sm text-slate-500 dark:text-slate-400">
          {loading ? '…' : `${facturas.length} factura(s) por cobrar`}
        </p>
      </PageHeader>

      {success && (
        <div className="card mb-4 flex items-center gap-2 border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">
          <Coins size={18} weight="duotone" />
          {success}
        </div>
      )}
      {error && (
        <div className="card mb-4 flex items-center gap-2 border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">
          <WarningCircle size={18} weight="duotone" />
          {error}
        </div>
      )}

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard label="Facturas por cobrar" value={facturas.length} icon={Receipt} tone="amber" />
        <StatCard
          label="Deuda pendiente"
          value={totalDeuda ? formatMoney(totalDeuda) : '—'}
          icon={Coins}
          tone="red"
          hint="Suma del total de facturas pendientes"
        />
        <StatCard
          label="Estados"
          value={ESTADOS_COBRABLE.filter((e) => facturas.some((f) => f.estado === e)).length}
          icon={Coins}
          tone="sky"
          hint="Pendiente, parcial y vencida"
        />
      </div>

      <DataTable
        columns={columns}
        data={facturas}
        keyField="id"
        loading={loading}
        emptyTitle="Sin facturas por cobrar"
        emptyText="No hay facturas pendientes, parciales o vencidas para registrar pagos."
      />

      <Modal
        open={target !== null}
        onClose={saving ? undefined : () => setTarget(null)}
        title={`Registrar pago · ${target?.numero || ''}`}
        size="md"
        footer={
          <>
            <Button variant="secondary" onClick={() => setTarget(null)} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="form-pago" disabled={saving || deudaPendiente <= 0}>
              {saving ? <Spinner size={14} /> : null}
              {saving ? 'Guardando…' : 'Registrar pago'}
            </Button>
          </>
        }
      >
        <div className="space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 px-4 py-3 dark:border-slate-800">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-slate-900 dark:text-slate-100">{target?.clienteNombre}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Vence {formatDate(target?.vencimiento)} · {m3(target?.consumoM3)}
              </p>
            </div>
            <Badge tone={estadoTone(target?.estado)}>{ETIQUETA_ESTADO[target?.estado] || target?.estado}</Badge>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            <div className="rounded-lg bg-slate-100 px-4 py-3 dark:bg-slate-800">
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400 dark:text-slate-500">Total</p>
              <p className="num mt-0.5 text-base font-semibold text-slate-900 dark:text-slate-100">
                {formatMoney(target?.total)}
              </p>
            </div>
            <div className="rounded-lg bg-slate-100 px-4 py-3 dark:bg-slate-800">
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400 dark:text-slate-500">Pagado</p>
              <p className="num mt-0.5 text-base font-semibold text-emerald-600 dark:text-emerald-300">
                {formatMoney(totalPagado)}
              </p>
            </div>
            <div className="rounded-lg bg-accent-50 px-4 py-3 dark:bg-accent-500/10">
              <p className="text-xs font-medium uppercase tracking-wide text-accent-600 dark:text-accent-300">
                Deuda pendiente
              </p>
              <p className="num mt-0.5 text-base font-semibold text-accent-700 dark:text-accent-200">
                {formatMoney(deudaPendiente)}
              </p>
            </div>
          </div>

          {pagosLoading ? (
            <div className="space-y-2">
              {[0, 1].map((i) => (
                <div key={i} className="h-12 animate-pulse rounded-lg bg-slate-200 dark:bg-slate-800" />
              ))}
            </div>
          ) : pagos.length > 0 ? (
            <div>
              <h3 className="mb-2 text-sm font-semibold">Pagos registrados</h3>
              <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200 dark:divide-slate-800 dark:border-slate-800">
                {pagos.map((p) => (
                  <li key={p.id} className="flex items-center justify-between gap-3 px-4 py-2.5">
                    <div>
                      <p className="num text-sm font-semibold text-slate-900 dark:text-slate-100">
                        {formatMoney(p.monto)}
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        {formatDate(p.fechaPago)} · {ETIQUETA_METODO[p.metodo] || p.metodo}
                      </p>
                    </div>
                    {p.referencia && (
                      <span className="max-w-[180px] truncate text-xs text-slate-500 dark:text-slate-400">
                        {p.referencia}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <EmptyState icon={Coins} title="Sin pagos" text="Esta factura aún no tiene pagos registrados." />
          )}

          {deudaPendiente <= 0 ? (
            <p className="rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300">
              Esta factura ya está totalmente pagada.
            </p>
          ) : (
            <form id="form-pago" onSubmit={registrar} className="grid gap-4 sm:grid-cols-2">
              {formError && (
                <p className="text-sm text-red-600 dark:text-red-400 sm:col-span-2">{formError}</p>
              )}
              <Field label="Monto" required>
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  className={inputCls}
                  value={form.monto}
                  onChange={(e) => setForm({ ...form, monto: e.target.value })}
                  placeholder={String(Number(deudaPendiente.toFixed(2)))}
                  required
                />
              </Field>
              <Field label="Método de pago" required>
                <select
                  className={inputCls}
                  value={form.metodo}
                  onChange={(e) => setForm({ ...form, metodo: e.target.value })}
                >
                  {Object.entries(ETIQUETA_METODO).map(([v, l]) => (
                    <option key={v} value={v}>
                      {l}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Referencia" hint="Opcional" className="sm:col-span-2">
                <input
                  className={inputCls}
                  value={form.referencia}
                  onChange={(e) => setForm({ ...form, referencia: e.target.value })}
                  placeholder="Ej. N.º de consignación"
                  maxLength={100}
                />
              </Field>
            </form>
          )}
        </div>
      </Modal>
    </div>
  )
}