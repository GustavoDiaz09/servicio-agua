import { useCallback, useEffect, useState } from 'react'
import { ChartLineUp, MapTrifold, Receipt, UsersThree, Warning } from '@phosphor-icons/react'
import { api } from '../api/client'
import { formatDate, formatMoney, m3 } from '../lib/format'
import {
  Badge,
  Button,
  DataTable,
  EmptyState,
  Field,
  Modal,
  PageHeader,
  Spinner,
  estadoTone,
  inputCls,
} from '../components/ui'

const SECCIONES = [
  { id: 'consumo', label: 'Consumo por sector', icon: MapTrifold },
  { id: 'ventas', label: 'Ventas por periodo', icon: Receipt },
  { id: 'morosos', label: 'Clientes morosos', icon: UsersThree },
  { id: 'fugas', label: 'Fugas activas', icon: Warning },
]

const SEVERIDAD_LABEL = { BAJA: 'Baja', MEDIA: 'Media', ALTA: 'Alta', CRITICA: 'Crítica' }
const SEVERIDAD_TONE = { BAJA: 'sky', MEDIA: 'amber', ALTA: 'red', CRITICA: 'red' }
const ESTADO_FUGA_LABEL = { REPORTADA: 'Reportada', EN_CURSO: 'En curso', RESUELTA: 'Resuelta' }

const mesActual = () => {
  const hoy = new Date()
  const anio = hoy.getFullYear()
  const mes = String(hoy.getMonth() + 1).padStart(2, '0')
  const ultimo = String(new Date(anio, hoy.getMonth() + 1, 0).getDate()).padStart(2, '0')
  return { desde: `${anio}-${mes}-01`, hasta: `${anio}-${mes}-${ultimo}` }
}

function Barra({ valor, maximo }) {
  const pct = maximo > 0 ? Math.round((Number(valor) / maximo) * 100) : 0
  return (
    <div className="flex items-center gap-3">
      <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
        <div className="h-full rounded-full bg-accent-500" style={{ width: `${pct}%` }} />
      </div>
      <span className="num w-24 shrink-0 text-right text-sm text-slate-700 dark:text-slate-200">{m3(valor)}</span>
    </div>
  )
}

export default function Reportes() {
  const [seccion, setSeccion] = useState('consumo')
  const [rango, setRango] = useState(mesActual)
  const [severidad, setSeveridad] = useState('')

  const [data, setData] = useState({ consumo: [], ventas: [], morosos: [], fugas: [] })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const [detalle, setDetalle] = useState(null)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      if (seccion === 'consumo') {
        const consumo = await api.get(`/reportes/consumo-por-sector?desde=${rango.desde}&hasta=${rango.hasta}`)
        setData((d) => ({ ...d, consumo }))
      } else if (seccion === 'ventas') {
        const ventas = await api.get(`/reportes/ventas-por-periodo?desde=${rango.desde}&hasta=${rango.hasta}`)
        setData((d) => ({ ...d, ventas }))
      } else if (seccion === 'morosos') {
        const morosos = await api.get('/reportes/clientes-morosos')
        setData((d) => ({ ...d, morosos }))
      } else if (seccion === 'fugas') {
        const params = severidad ? `?severidad=${severidad}` : ''
        const fugas = await api.get(`/reportes/fugas-activas${params}`)
        setData((d) => ({ ...d, fugas }))
      }
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [seccion, rango.desde, rango.hasta, severidad])

  useEffect(() => {
    cargar()
  }, [cargar])

  const consumo = data.consumo || []
  const ventas = data.ventas || []
  const morosos = data.morosos || []
  const fugas = data.fugas || []

  const consumoTotal = consumo.reduce((s, c) => s + Number(c.consumoTotal || 0), 0)
  const ventasTotal = ventas.reduce((s, v) => s + Number(v.totalVendido || 0), 0)
  const deudaMorosos = morosos.reduce((s, m) => s + Number(m.deudaTotal || 0), 0)
  const maxConsumo = Math.max(...consumo.map((c) => Number(c.consumoTotal) || 0), 1)

  return (
    <div>
      <PageHeader title="Reportes" subtitle="Consultas de agregación sobre consumo, ventas, morosidad y red">
        <div className="inline-flex flex-wrap rounded-lg bg-slate-100 p-0.5 dark:bg-slate-800">
          {SECCIONES.map((s) => (
            <button
              key={s.id}
              onClick={() => setSeccion(s.id)}
              className={
                seccion === s.id
                  ? 'rounded-md bg-white px-3 py-1.5 text-sm font-medium text-slate-900 shadow-sm dark:bg-slate-900 dark:text-slate-100'
                  : 'rounded-md px-3 py-1.5 text-sm font-medium text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200'
              }
            >
              {s.label}
            </button>
          ))}
        </div>
      </PageHeader>

      {(seccion === 'consumo' || seccion === 'ventas') && (
        <div className="mb-4 flex flex-wrap items-end gap-3">
          <Field label="Desde">
            <input
              type="date"
              className={`${inputCls} w-44`}
              value={rango.desde}
              onChange={(e) => setRango((r) => ({ ...r, desde: e.target.value }))}
            />
          </Field>
          <Field label="Hasta">
            <input
              type="date"
              className={`${inputCls} w-44`}
              value={rango.hasta}
              onChange={(e) => setRango((r) => ({ ...r, hasta: e.target.value }))}
            />
          </Field>
          <Button
            variant="secondary"
            onClick={() => setRango(mesActual())}
          >
            Este mes
          </Button>
        </div>
      )}

      {seccion === 'fugas' && (
        <div className="mb-4 flex flex-wrap items-end gap-3">
          <Field label="Severidad">
            <select
              className={`${inputCls} w-44`}
              value={severidad}
              onChange={(e) => setSeveridad(e.target.value)}
            >
              <option value="">Todas</option>
              <option value="BAJA">Baja</option>
              <option value="MEDIA">Media</option>
              <option value="ALTA">Alta</option>
              <option value="CRITICA">Crítica</option>
            </select>
          </Field>
        </div>
      )}

      {error && (
        <div className="card mb-4 rounded-lg border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">
          {error}
        </div>
      )}

      {seccion === 'consumo' && (
        <>
          <div className="mb-4 flex items-center gap-2">
            <MapTrifold size={18} weight="duotone" className="text-accent-600 dark:text-accent-300" />
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Consumo total en el período: <span className="num font-semibold">{m3(consumoTotal)}</span>
            </p>
          </div>
          <DataTable
            columns={[
              { key: 'nombre', header: 'Sector' },
              {
                key: 'consumoTotal',
                header: 'Consumo',
                render: (r) => <Barra valor={r.consumoTotal} maximo={maxConsumo} />,
              },
            ]}
            data={consumo}
            keyField="sector"
            loading={loading}
            emptyTitle="Sin consumo"
            emptyText="No hay lecturas registradas en el período seleccionado."
          />
        </>
      )}

      {seccion === 'ventas' && (
        <>
          <div className="mb-4 flex items-center gap-2">
            <Receipt size={18} weight="duotone" className="text-emerald-600 dark:text-emerald-300" />
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Total vendido en el período: <span className="num font-semibold">{formatMoney(ventasTotal)}</span>
            </p>
          </div>
          <DataTable
            columns={[
              { key: 'periodo', header: 'Período', render: (r) => <span className="font-medium">{r.periodo}</span> },
              {
                key: 'totalVendido',
                header: 'Total vendido',
                align: 'right',
                render: (r) => <span className="num font-medium text-emerald-700 dark:text-emerald-300">{formatMoney(r.totalVendido)}</span>,
              },
            ]}
            data={ventas}
            keyField="periodo"
            loading={loading}
            emptyTitle="Sin ventas"
            emptyText="No hay pagos registrados en el período seleccionado."
          />
        </>
      )}

      {seccion === 'morosos' && (
        <>
          <div className="mb-4 flex items-center gap-2">
            <UsersThree size={18} weight="duotone" className="text-red-600 dark:text-red-300" />
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Deuda total de morosos: <span className="num font-semibold text-red-600 dark:text-red-400">{formatMoney(deudaMorosos)}</span>
            </p>
          </div>
          {morosos.length === 0 ? (
            loading ? null : (
              <EmptyState title="Sin morosos" text="No hay clientes con facturas pendientes." />
            )
          ) : (
            <DataTable
              columns={[
                { key: 'nombre', header: 'Cliente', render: (r) => <span className="font-medium text-slate-900 dark:text-slate-100">{r.nombre}</span> },
                { key: 'documento', header: 'Documento' },
                {
                  key: 'deudaTotal',
                  header: 'Deuda',
                  align: 'right',
                  render: (r) => <span className="num font-medium text-red-600 dark:text-red-400">{formatMoney(r.deudaTotal)}</span>,
                },
                { key: 'cantidadFacturas', header: 'Facturas', align: 'right', render: (r) => <span className="num">{r.cantidadFacturas}</span> },
                {
                  key: 'acciones',
                  header: '',
                  align: 'right',
                  render: (r) => (
                    <Button variant="ghost" className="!px-2.5" onClick={() => setDetalle(r)}>
                      <ChartLineUp size={16} weight="bold" />
                      Ver facturas
                    </Button>
                  ),
                },
              ]}
              data={morosos}
              keyField="clienteId"
              loading={loading}
            />
          )}
        </>
      )}

      {seccion === 'fugas' && (
        <DataTable
          columns={[
            { key: 'ubicacion', header: 'Ubicación', render: (r) => <span className="font-medium text-slate-900 dark:text-slate-100">{r.ubicacion}</span> },
            { key: 'sector', header: 'Sector' },
            {
              key: 'severidad',
              header: 'Severidad',
              render: (r) => <Badge tone={SEVERIDAD_TONE[r.severidad]}>{SEVERIDAD_LABEL[r.severidad] || r.severidad}</Badge>,
            },
            {
              key: 'estado',
              header: 'Estado',
              render: (r) => <Badge tone={estadoTone(r.estado)}>{ESTADO_FUGA_LABEL[r.estado] || r.estado}</Badge>,
            },
            { key: 'fechaReporte', header: 'Reporte', render: (r) => formatDate(r.fechaReporte) },
          ]}
          data={fugas}
          keyField="id"
          loading={loading}
          emptyTitle="Sin fugas activas"
          emptyText={severidad ? 'No hay fugas activas con la severidad seleccionada.' : 'Toda la red se encuentra operando con normalidad.'}
        />
      )}

      {loading && seccion === 'morosos' && morosos.length === 0 && (
        <div className="card flex items-center justify-center gap-2 py-10 text-sm text-slate-500 dark:text-slate-400">
          <Spinner size={16} />
          Cargando reporte…
        </div>
      )}

      <Modal
        open={detalle !== null}
        onClose={() => setDetalle(null)}
        title={`Facturas de ${detalle?.nombre || ''}`}
        size="md"
        footer={
          <Button variant="secondary" onClick={() => setDetalle(null)}>
            Cerrar
          </Button>
        }
      >
        {detalle && (
          <div className="space-y-4">
            <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 px-4 py-3 dark:border-slate-800">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-slate-900 dark:text-slate-100">{detalle.nombre}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400">{detalle.documento}</p>
              </div>
              <span className="num text-sm font-semibold text-red-600 dark:text-red-400">{formatMoney(detalle.deudaTotal)}</span>
            </div>
            <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200 dark:divide-slate-800 dark:border-slate-800">
              {detalle.facturas.map((f) => (
                <li key={f.id} className="flex items-center justify-between gap-3 px-4 py-3">
                  <div className="min-w-0">
                    <p className="num text-sm font-semibold text-slate-900 dark:text-slate-100">{f.numero}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {formatDate(f.periodoDesde)} → {formatDate(f.periodoHasta)} · Vence {formatDate(f.vencimiento)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="num text-sm font-medium">{formatMoney(f.total)}</span>
                    <Badge tone={estadoTone(f.estado)}>{f.estado}</Badge>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}
      </Modal>
    </div>
  )
}