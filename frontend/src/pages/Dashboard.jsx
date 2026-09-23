import { useEffect, useState } from 'react'
import { Coins, Drop, MapTrifold, UsersThree } from '@phosphor-icons/react'
import { api } from '../api/client'
import { Badge, DataTable, EmptyState, PageHeader, Skeleton, StatCard } from '../components/ui'
import { formatDate, formatMoney, m3 } from '../lib/format'

const etiqueta = (valor) => {
  if (!valor) return '—'
  return String(valor)
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((palabra) => palabra.charAt(0).toUpperCase() + palabra.slice(1))
    .join(' ')
}

const severidadTone = (severidad) => {
  const s = String(severidad || '').toUpperCase()
  if (['ALTA', 'CRITICA'].includes(s)) return 'red'
  if (s === 'MEDIA') return 'amber'
  return 'sky'
}

const mesActual = () => {
  const hoy = new Date()
  const anio = hoy.getFullYear()
  const mes = String(hoy.getMonth() + 1).padStart(2, '0')
  const ultimo = String(new Date(anio, hoy.getMonth() + 1, 0).getDate()).padStart(2, '0')
  return { desde: `${anio}-${mes}-01`, hasta: `${anio}-${mes}-${ultimo}` }
}

const sumar = (lista, clave) => lista.reduce((total, item) => total + Number(item[clave] || 0), 0)

const mensajeError = (motivo) => motivo?.message || 'No se pudo cargar este dato.'

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

function ListaEsqueleto() {
  return (
    <div className="space-y-5 p-5">
      {[1, 2, 3].map((i) => (
        <div key={i} className="flex items-center justify-between gap-3">
          <div className="space-y-1.5">
            <Skeleton className="h-3.5 w-28" />
            <Skeleton className="h-3 w-20" />
          </div>
          <Skeleton className="h-4 w-16" />
        </div>
      ))}
    </div>
  )
}

function Panel({ titulo, cantidad, tone, children }) {
  return (
    <div className="card">
      <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
        <h3 className="text-sm font-semibold">{titulo}</h3>
        {cantidad !== undefined && <Badge tone={tone}>{cantidad}</Badge>}
      </div>
      {children}
    </div>
  )
}

export default function Dashboard() {
  const [rango] = useState(mesActual)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState({ consumo: '', ventas: '', morosos: '', fugas: '' })
  const [data, setData] = useState({ consumo: [], ventas: [], morosos: [], fugas: [] })

  useEffect(() => {
    let vivo = true
    setCargando(true)
    setError({})
    const params = `desde=${rango.desde}&hasta=${rango.hasta}`
    Promise.allSettled([
      api.get(`/reportes/consumo-por-sector?${params}`),
      api.get(`/reportes/ventas-por-periodo?${params}`),
      api.get('/reportes/clientes-morosos'),
      api.get('/reportes/fugas-activas'),
    ]).then(([consumo, ventas, morosos, fugas]) => {
      if (!vivo) return
      setData({
        consumo: consumo.status === 'fulfilled' ? consumo.value : [],
        ventas: ventas.status === 'fulfilled' ? ventas.value : [],
        morosos: morosos.status === 'fulfilled' ? morosos.value : [],
        fugas: fugas.status === 'fulfilled' ? fugas.value : [],
      })
      setError({
        consumo: consumo.status === 'rejected' ? mensajeError(consumo.reason) : '',
        ventas: ventas.status === 'rejected' ? mensajeError(ventas.reason) : '',
        morosos: morosos.status === 'rejected' ? mensajeError(morosos.reason) : '',
        fugas: fugas.status === 'rejected' ? mensajeError(fugas.reason) : '',
      })
      setCargando(false)
    })
    return () => {
      vivo = false
    }
  }, [rango.desde, rango.hasta])

  const consumoMes = data.consumo.length ? sumar(data.consumo, 'consumoTotal') : null
  const ventasMes = data.ventas.length ? sumar(data.ventas, 'totalVendido') : null
  const deudaMorosos = data.morosos.length ? sumar(data.morosos, 'deudaTotal') : null
  const maxConsumo = Math.max(...data.consumo.map((c) => Number(c.consumoTotal) || 0), 1)
  const topMorosos = [...data.morosos].sort((a, b) => Number(b.deudaTotal) - Number(a.deudaTotal)).slice(0, 5)

  const consumoColumns = [
    { key: 'nombre', header: 'Sector' },
    { key: 'consumoTotal', header: 'Consumo', render: (r) => <Barra valor={r.consumoTotal} maximo={maxConsumo} /> },
  ]

  return (
    <div>
      <PageHeader
        title="Dashboard"
        subtitle={`Resumen del mes actual · ${formatDate(rango.desde)} – ${formatDate(rango.hasta)}`}
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Consumo del mes"
          value={cargando ? <Skeleton className="h-7 w-20" /> : consumoMes == null ? '—' : m3(consumoMes)}
          icon={Drop}
          tone="sky"
          hint="Suma de consumo por sector"
        />
        <StatCard
          label="Ventas del mes"
          value={cargando ? <Skeleton className="h-7 w-24" /> : ventasMes == null ? '—' : formatMoney(ventasMes)}
          icon={Coins}
          tone="green"
          hint="Pagos acreditados en el período"
        />
        <StatCard
          label="Clientes morosos"
          value={cargando ? <Skeleton className="h-7 w-10" /> : error.morosos ? '—' : data.morosos.length}
          icon={UsersThree}
          tone="red"
          hint={deudaMorosos == null ? undefined : `Deuda total: ${formatMoney(deudaMorosos)}`}
        />
        <StatCard
          label="Fugas activas"
          value={cargando ? <Skeleton className="h-7 w-10" /> : error.fugas ? '—' : data.fugas.length}
          icon={MapTrifold}
          tone="amber"
          hint="Pendientes de resolución"
        />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-6 xl:grid-cols-3">
        <section className="min-w-0 xl:col-span-2">
          <h2 className="mb-3 text-base font-semibold">Consumo por sector</h2>
          <DataTable
            columns={consumoColumns}
            data={data.consumo}
            keyField="sector"
            loading={cargando}
            emptyTitle="Sin consumo"
            emptyText="No hay lecturas registradas en el mes actual."
          />
          {error.consumo && <p className="mt-2 text-sm text-red-600 dark:text-red-400">{error.consumo}</p>}
        </section>

        <div className="space-y-6">
          <Panel titulo="Morosidad" cantidad={error.morosos ? undefined : data.morosos.length} tone="red">
            {cargando ? (
              <ListaEsqueleto />
            ) : error.morosos ? (
              <p className="p-5 text-sm text-red-600 dark:text-red-400">{error.morosos}</p>
            ) : topMorosos.length === 0 ? (
              <EmptyState title="Sin morosos" text="No hay clientes con facturas pendientes." />
            ) : (
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                {topMorosos.map((m) => (
                  <div key={m.clienteId} className="flex items-center justify-between gap-3 px-5 py-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{m.nombre}</p>
                      <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{m.documento}</p>
                    </div>
                    <span className="num shrink-0 text-sm font-medium text-red-600 dark:text-red-400">
                      {formatMoney(m.deudaTotal)}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </Panel>

          <Panel titulo="Fugas activas" cantidad={error.fugas ? undefined : data.fugas.length} tone="amber">
            {cargando ? (
              <ListaEsqueleto />
            ) : error.fugas ? (
              <p className="p-5 text-sm text-red-600 dark:text-red-400">{error.fugas}</p>
            ) : data.fugas.length === 0 ? (
              <EmptyState title="Sin fugas activas" text="Toda la red se encuentra operando con normalidad." />
            ) : (
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                {data.fugas.map((f) => (
                  <div key={f.id} className="flex items-start justify-between gap-3 px-5 py-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{f.ubicacion}</p>
                      <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{f.sector}</p>
                    </div>
                    <Badge tone={severidadTone(f.severidad)}>{etiqueta(f.severidad)}</Badge>
                  </div>
                ))}
              </div>
            )}
          </Panel>
        </div>
      </div>
    </div>
  )
}