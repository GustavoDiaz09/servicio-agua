import { useCallback, useEffect, useState } from 'react'
import { CheckCircle, Eye, Hourglass, Plus, Receipt, Trash, WarningCircle, X } from '@phosphor-icons/react'
import { api } from '../api/client'
import { formatDate, formatMoney, m3 } from '../lib/format'
import {
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  EmptyState,
  Field,
  Modal,
  PageHeader,
  Spinner,
  estadoTone,
  inputCls,
} from '../components/ui'

const ESTADOS = ['PENDIENTE', 'PARCIAL', 'PAGADA', 'VENCIDA', 'ANULADA']
const ETIQUETA_ESTADO = {
  PENDIENTE: 'Pendiente',
  PARCIAL: 'Parcial',
  PAGADA: 'Pagada',
  VENCIDA: 'Vencida',
  ANULADA: 'Anulada',
}
const ETIQUETA_METODO = { EFECTIVO: 'Efectivo', TRANSFERENCIA: 'Transferencia', TARJETA: 'Tarjeta', QR: 'QR' }

function etiquetaEstado(estado) {
  return ETIQUETA_ESTADO[estado] || estado
}

async function todasLasFacturas() {
  const grupos = await Promise.all(ESTADOS.map((e) => api.get('/facturas/estado/' + e)))
  return grupos.flat().sort((a, b) => (b.id ?? 0) - (a.id ?? 0))
}

const iconBtnCls =
  'inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200'

function CampoDetalle({ label, value }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-slate-400 dark:text-slate-500">{label}</p>
      <p className="mt-0.5 text-sm font-medium text-slate-900 dark:text-slate-100">{value}</p>
    </div>
  )
}

function Aviso({ tone = 'error', onClose, children }) {
  const toneCls =
    tone === 'success'
      ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300'
      : 'border-red-200 bg-red-50 text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300'
  const Icon = tone === 'success' ? CheckCircle : WarningCircle
  return (
    <div className={`flex items-start gap-2 rounded-lg border px-4 py-3 text-sm ${toneCls}`}>
      <Icon size={18} weight="duotone" className="mt-0.5 shrink-0" />
      <p className="flex-1">{children}</p>
      {onClose && (
        <button onClick={onClose} className="rounded-md p-0.5 transition-colors hover:bg-black/5">
          <X size={16} />
        </button>
      )}
    </div>
  )
}

export default function Facturas() {
  const [facturas, setFacturas] = useState([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState(null)
  const [filtro, setFiltro] = useState('TODOS')

  const [clientes, setClientes] = useState([])

  const [genOpen, setGenOpen] = useState(false)
  const [genForm, setGenForm] = useState({ clienteId: '', periodoDesde: '', periodoHasta: '', vencimiento: '' })
  const [genErrors, setGenErrors] = useState({})
  const [genError, setGenError] = useState(null)
  const [generating, setGenerating] = useState(false)
  const [generada, setGenerada] = useState(null)

  const [detalle, setDetalle] = useState(null)
  const [pagos, setPagos] = useState([])
  const [pagosLoading, setPagosLoading] = useState(false)

  const [anularTarget, setAnularTarget] = useState(null)
  const [anularBusy, setAnularBusy] = useState(false)

  const [marcando, setMarcando] = useState(false)
  const [aviso, setAviso] = useState(null)

  const aplicarFiltro = useCallback(async (estado) => {
    setLoading(true)
    setListError(null)
    try {
      const data = estado === 'TODOS' ? await todasLasFacturas() : await api.get('/facturas/estado/' + estado)
      setFacturas(data)
    } catch (e) {
      setListError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    aplicarFiltro(filtro)
  }, [aplicarFiltro, filtro])

  useEffect(() => {
    api.get('/clientes')
      .then(setClientes)
      .catch(() => {})
  }, [])

  function abrirDetalle(factura) {
    setDetalle(factura)
    setPagos([])
    setPagosLoading(true)
    api.get('/pagos/factura/' + factura.id)
      .then(setPagos)
      .catch(() => setPagos([]))
      .finally(() => setPagosLoading(false))
  }

  function puedeAnular(factura) {
    return factura.estado !== 'PAGADA' && factura.estado !== 'ANULADA'
  }

  async function confirmarAnulacion() {
    setAnularBusy(true)
    try {
      const r = await api.put('/facturas/' + anularTarget.id + '/anular')
      setAviso({ tone: 'success', msg: `Factura ${r.numero} anulada correctamente` })
      setAnularTarget(null)
      setDetalle(null)
      aplicarFiltro(filtro)
    } catch (e) {
      setAviso({ tone: 'error', msg: e.message })
      setAnularTarget(null)
    } finally {
      setAnularBusy(false)
    }
  }

  async function marcarVencidas() {
    setMarcando(true)
    try {
      const cantidad = await api.post('/facturas/marcar-vencidas')
      setAviso({ tone: 'success', msg: `${cantidad} factura(s) marcada(s) como vencidas` })
      aplicarFiltro(filtro)
    } catch (e) {
      setAviso({ tone: 'error', msg: e.message })
    } finally {
      setMarcando(false)
    }
  }

  function validarGeneracion() {
    const errs = {}
    if (!genForm.clienteId) errs.clienteId = 'El cliente es obligatorio'
    if (!genForm.periodoDesde) errs.periodoDesde = 'Obligatorio'
    if (!genForm.periodoHasta) errs.periodoHasta = 'Obligatorio'
    if (!genForm.vencimiento) errs.vencimiento = 'Obligatorio'
    if (genForm.periodoDesde && genForm.periodoHasta && genForm.periodoHasta < genForm.periodoDesde) {
      errs.periodoHasta = 'El período hasta no puede ser anterior al período desde'
    }
    if (genForm.periodoHasta && genForm.vencimiento && genForm.vencimiento < genForm.periodoHasta) {
      errs.vencimiento = 'El vencimiento no puede ser anterior al fin del período facturado'
    }
    return errs
  }

  async function generarFactura() {
    const errs = validarGeneracion()
    setGenErrors(errs)
    if (Object.keys(errs).length) return
    setGenerating(true)
    setGenError(null)
    try {
      const body = {
        clienteId: Number(genForm.clienteId),
        periodoDesde: genForm.periodoDesde,
        periodoHasta: genForm.periodoHasta,
        vencimiento: genForm.vencimiento,
      }
      const factura = await api.post('/facturas/generar', body)
      setGenerada(factura)
      aplicarFiltro(filtro)
    } catch (e) {
      setGenError(e.message)
    } finally {
      setGenerating(false)
    }
  }

  function cerrarGeneracion() {
    setGenOpen(false)
    setGenerada(null)
    setGenError(null)
    setGenErrors({})
    setGenForm({ clienteId: '', periodoDesde: '', periodoHasta: '', vencimiento: '' })
  }

  function resetearGeneracion() {
    setGenerada(null)
    setGenError(null)
    setGenErrors({})
    setGenForm({ clienteId: '', periodoDesde: '', periodoHasta: '', vencimiento: '' })
  }

  const columnas = [
    {
      key: 'numero',
      header: 'Número',
      render: (f) => <span className="font-medium text-slate-900 dark:text-slate-100">{f.numero}</span>,
    },
    { key: 'clienteNombre', header: 'Cliente' },
    { key: 'consumoM3', header: 'Consumo', render: (f) => <span className="num">{m3(f.consumoM3)}</span> },
    {
      key: 'subtotal',
      header: 'Subtotal',
      align: 'right',
      render: (f) => <span className="num">{formatMoney(f.subtotal)}</span>,
    },
    {
      key: 'cargoFijo',
      header: 'Cargo fijo',
      align: 'right',
      render: (f) => <span className="num">{formatMoney(f.cargoFijo)}</span>,
    },
    {
      key: 'total',
      header: 'Total',
      align: 'right',
      render: (f) => (
        <span className="num font-semibold text-slate-900 dark:text-slate-100">{formatMoney(f.total)}</span>
      ),
    },
    { key: 'vencimiento', header: 'Vencimiento', render: (f) => formatDate(f.vencimiento) },
    {
      key: 'estado',
      header: 'Estado',
      render: (f) => (
        <Badge tone={estadoTone(f.estado)}>{etiquetaEstado(f.estado)}</Badge>
      ),
    },
    {
      key: 'acciones',
      header: '',
      render: (f) => (
        <div className="flex justify-end gap-1">
          <button
            onClick={(e) => {
              e.stopPropagation()
              abrirDetalle(f)
            }}
            className={iconBtnCls}
            title="Ver detalle"
          >
            <Eye size={16} />
          </button>
          {puedeAnular(f) && (
            <button
              onClick={(e) => {
                e.stopPropagation()
                setAnularTarget(f)
              }}
              className={`${iconBtnCls} text-red-500 hover:bg-red-50 hover:text-red-600 dark:text-red-400 dark:hover:bg-red-500/10`}
              title="Anular"
            >
              <Trash size={16} />
            </button>
          )}
        </div>
      ),
    },
  ]

  const totalPagado = pagos.reduce((s, p) => s + (Number(p.monto) || 0), 0)

  return (
    <div>
      <PageHeader
        title="Facturas"
        subtitle="Emisión, anulación y seguimiento del estado de las facturas."
      >
        <Button variant="secondary" onClick={marcarVencidas} disabled={marcando}>
          {marcando ? <Spinner size={16} /> : <Hourglass size={16} weight="bold" />}
          {marcando ? 'Marcando…' : 'Marcar vencidas'}
        </Button>
        <Button onClick={() => setGenOpen(true)}>
          <Plus size={16} weight="bold" />
          Generar factura
        </Button>
      </PageHeader>

      {aviso && (
        <div className="mb-5">
          <Aviso tone={aviso.tone} onClose={() => setAviso(null)}>
            {aviso.msg}
          </Aviso>
        </div>
      )}

      {listError && (
        <div className="mb-5">
          <Aviso onClose={() => setListError(null)}>{listError}</Aviso>
        </div>
      )}

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <Field label="Estado" className="w-56">
          <select className={inputCls} value={filtro} onChange={(e) => setFiltro(e.target.value)}>
            <option value="TODOS">Todas</option>
            {ESTADOS.map((e) => (
              <option key={e} value={e}>
                {etiquetaEstado(e)}
              </option>
            ))}
          </select>
        </Field>
        <p className="num pb-2 text-sm text-slate-500 dark:text-slate-400">
          {loading ? '…' : `${facturas.length} factura(s)`}
        </p>
      </div>

      <DataTable
        columns={columnas}
        data={facturas}
        keyField="id"
        loading={loading}
        onRowClick={abrirDetalle}
        emptyTitle="Sin facturas"
        emptyText="No hay facturas para el filtro seleccionado."
      />

      <Modal
        open={genOpen}
        onClose={generating ? undefined : cerrarGeneracion}
        title="Generar factura"
        size="md"
        footer={
          generada ? (
            <>
              <Button variant="secondary" onClick={resetearGeneracion} disabled={generating}>
                Generar otra
              </Button>
              <Button onClick={cerrarGeneracion}>
                <CheckCircle size={16} weight="bold" />
                Listo
              </Button>
            </>
          ) : (
            <>
              <Button variant="secondary" onClick={cerrarGeneracion} disabled={generating}>
                Cancelar
              </Button>
              <Button onClick={generarFactura} disabled={generating}>
                {generating ? <Spinner size={16} /> : <Plus size={16} weight="bold" />}
                {generating ? 'Generando…' : 'Generar'}
              </Button>
            </>
          )
        }
      >
        {generada ? (
          <div className="space-y-4">
            <div className="flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 dark:border-emerald-500/30 dark:bg-emerald-500/10">
              <CheckCircle size={22} weight="duotone" className="mt-0.5 shrink-0 text-emerald-600 dark:text-emerald-300" />
              <div>
                <p className="text-sm font-semibold text-emerald-800 dark:text-emerald-200">
                  Factura generada exitosamente
                </p>
                <p className="text-sm text-emerald-700 dark:text-emerald-300">{generada.numero}</p>
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <CampoDetalle label="Cliente" value={generada.clienteNombre} />
              <CampoDetalle label="Consumo" value={m3(generada.consumoM3)} />
              <CampoDetalle label="Período" value={`${formatDate(generada.periodoDesde)} → ${formatDate(generada.periodoHasta)}`} />
              <CampoDetalle label="Vencimiento" value={formatDate(generada.vencimiento)} />
              <CampoDetalle label="Subtotal" value={<span className="num">{formatMoney(generada.subtotal)}</span>} />
              <CampoDetalle label="Cargo fijo" value={<span className="num">{formatMoney(generada.cargoFijo)}</span>} />
            </div>
            <div className="flex items-center justify-between rounded-xl border border-accent-200 bg-accent-50 px-4 py-3 dark:border-accent-500/30 dark:bg-accent-500/10">
              <span className="text-sm font-medium text-accent-700 dark:text-accent-300">Total a pagar</span>
              <span className="num text-xl font-semibold text-accent-800 dark:text-accent-200">
                {formatMoney(generada.total)}
              </span>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <Field label="Cliente" required error={genErrors.clienteId && genErrors.clienteId}>
              <select
                className={inputCls}
                value={genForm.clienteId}
                onChange={(e) => setGenForm({ ...genForm, clienteId: e.target.value })}
              >
                <option value="">Selecciona un cliente</option>
                {clientes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nombre} · {c.documento}
                  </option>
                ))}
              </select>
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Período desde" required error={genErrors.periodoDesde}>
                <input
                  type="date"
                  className={inputCls}
                  value={genForm.periodoDesde}
                  onChange={(e) => setGenForm({ ...genForm, periodoDesde: e.target.value })}
                />
              </Field>
              <Field label="Período hasta" required error={genErrors.periodoHasta}>
                <input
                  type="date"
                  className={inputCls}
                  value={genForm.periodoHasta}
                  onChange={(e) => setGenForm({ ...genForm, periodoHasta: e.target.value })}
                />
              </Field>
            </div>
            <Field
              label="Vencimiento"
              required
              error={genErrors.vencimiento}
              hint="Debe ser igual o posterior al fin del período facturado."
            >
              <input
                type="date"
                className={inputCls}
                value={genForm.vencimiento}
                onChange={(e) => setGenForm({ ...genForm, vencimiento: e.target.value })}
              />
            </Field>
            {genError && (
              <Aviso onClose={() => setGenError(null)}>
                {genError}
              </Aviso>
            )}
          </div>
        )}
      </Modal>

      <Modal
        open={detalle !== null}
        onClose={() => setDetalle(null)}
        title={detalle ? detalle.numero : ''}
        size="md"
        footer={
          detalle && (
            <>
              {puedeAnular(detalle) && (
                <Button variant="danger" onClick={() => setAnularTarget(detalle)}>
                  <Trash size={16} weight="bold" />
                  Anular factura
                </Button>
              )}
              <Button variant="secondary" onClick={() => setDetalle(null)}>
                Cerrar
              </Button>
            </>
          )
        }
      >
        {detalle && (
          <div className="space-y-5">
            <div className="flex items-center justify-between gap-3">
              <Badge tone={estadoTone(detalle.estado)}>{etiquetaEstado(detalle.estado)}</Badge>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <CampoDetalle label="Cliente" value={detalle.clienteNombre} />
              <CampoDetalle label="Consumo" value={m3(detalle.consumoM3)} />
              <CampoDetalle label="Período" value={`${formatDate(detalle.periodoDesde)} → ${formatDate(detalle.periodoHasta)}`} />
              <CampoDetalle label="Vencimiento" value={formatDate(detalle.vencimiento)} />
              <CampoDetalle label="Subtotal" value={<span className="num">{formatMoney(detalle.subtotal)}</span>} />
              <CampoDetalle label="Cargo fijo" value={<span className="num">{formatMoney(detalle.cargoFijo)}</span>} />
            </div>
            <div className="flex items-center justify-between rounded-xl border border-accent-200 bg-accent-50 px-4 py-3 dark:border-accent-500/30 dark:bg-accent-500/10">
              <span className="text-sm font-medium text-accent-700 dark:text-accent-300">Total de la factura</span>
              <span className="num text-xl font-semibold text-accent-800 dark:text-accent-200">
                {formatMoney(detalle.total)}
              </span>
            </div>
            <div>
              <div className="mb-3 flex items-center justify-between">
                <h3 className="text-sm font-semibold">Pagos registrados</h3>
                {!pagosLoading && pagos.length > 0 && (
                  <span className="num text-xs text-slate-500 dark:text-slate-400">
                    Pagado: {formatMoney(totalPagado)}
                  </span>
                )}
              </div>
              {pagosLoading ? (
                <div className="space-y-2">
                  {[0, 1, 2].map((i) => (
                    <div key={i} className="h-12 animate-pulse rounded-lg bg-slate-200 dark:bg-slate-800" />
                  ))}
                </div>
              ) : pagos.length === 0 ? (
                <EmptyState
                  icon={Receipt}
                  title="Sin pagos"
                  text="Esta factura aún no tiene pagos registrados."
                />
              ) : (
                <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200 dark:divide-slate-800 dark:border-slate-800">
                  {pagos.map((p) => (
                    <li key={p.id} className="flex items-center justify-between gap-3 px-4 py-3">
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
              )}
            </div>
          </div>
        )}
      </Modal>

      <ConfirmDialog
        open={anularTarget !== null}
        onClose={() => setAnularTarget(null)}
        onConfirm={confirmarAnulacion}
        busy={anularBusy}
        title="Anular factura"
        confirmLabel="Anular"
        message={
          anularTarget
            ? `¿Deseas anular la factura ${anularTarget.numero} de ${anularTarget.clienteNombre}? Esta acción no se puede deshacer.`
            : ''
        }
      />
    </div>
  )
}