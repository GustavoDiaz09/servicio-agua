import { useCallback, useEffect, useState } from 'react'
import { ArrowClockwise, Plus, Prohibit, SignOut } from '@phosphor-icons/react'
import { api } from '../api/client'
import { formatDate } from '../lib/format'
import {
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  Field,
  Modal,
  PageHeader,
  Spinner,
  estadoTone,
  inputCls,
} from '../components/ui'

const etiquetaEstado = {
  ACTIVA: 'Activa',
  SUSPENDIDA: 'Suspendida',
  DADA_DE_BAJA: 'Dada de baja',
}

const ETAPAS_ESTADO = ['ACTIVA', 'SUSPENDIDA']

function ErrorBanner({ children }) {
  if (!children) return null
  return (
    <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">
      {children}
    </div>
  )
}

export default function Tomas() {
  const [tomas, setTomas] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [clientes, setClientes] = useState([])
  const [sectores, setSectores] = useState([])

  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState({ codigo: '', sectorId: '', clienteId: '' })
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const [estadoOpen, setEstadoOpen] = useState(false)
  const [estadoTarget, setEstadoTarget] = useState(null)
  const [nuevoEstado, setNuevoEstado] = useState('')
  const [estadoError, setEstadoError] = useState('')
  const [cambiando, setCambiando] = useState(false)

  const [bajaTarget, setBajaTarget] = useState(null)
  const [bajaError, setBajaError] = useState('')
  const [dandoBaja, setDandoBaja] = useState(false)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await api.get('/tomas')
      setTomas(res || [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    cargar()
  }, [cargar])

  async function cargarSelectores() {
    const [cl, se] = await Promise.all([api.get('/clientes'), api.get('/sectores')])
    setClientes(cl || [])
    setSectores(se || [])
  }

  async function abrirCrear() {
    setForm({ codigo: '', sectorId: '', clienteId: '' })
    setFormError('')
    setFormOpen(true)
    try {
      await cargarSelectores()
    } catch (e) {
      setFormError(e.message)
    }
  }

  function abrirEstado(t) {
    setEstadoTarget(t)
    setNuevoEstado(t.estado)
    setEstadoError('')
    setEstadoOpen(true)
  }

  async function guardar(e) {
    e.preventDefault()
    if (!form.codigo.trim() || !form.sectorId || !form.clienteId) {
      setFormError('Completá todos los campos obligatorios')
      return
    }
    setSaving(true)
    setFormError('')
    try {
      await api.post('/tomas', {
        codigo: form.codigo.trim(),
        sectorId: Number(form.sectorId),
        clienteId: Number(form.clienteId),
      })
      setFormOpen(false)
      await cargar()
    } catch (e) {
      setFormError(e.message)
    } finally {
      setSaving(false)
    }
  }

  async function guardarEstado() {
    setCambiando(true)
    setEstadoError('')
    try {
      await api.put(`/tomas/${estadoTarget.id}/estado`, { estado: nuevoEstado })
      setEstadoOpen(false)
      await cargar()
    } catch (e) {
      setEstadoError(e.message)
    } finally {
      setCambiando(false)
    }
  }

  function abrirBaja(t) {
    setBajaTarget(t)
    setBajaError('')
    setDandoBaja(false)
  }

  async function confirmarBaja() {
    setDandoBaja(true)
    setBajaError('')
    try {
      await api.put(`/tomas/${bajaTarget.id}/baja`)
      setBajaTarget(null)
      await cargar()
    } catch (e) {
      setBajaError(e.message)
    } finally {
      setDandoBaja(false)
    }
  }

  const opcionesEstado = estadoTarget
    ? ETAPAS_ESTADO.filter((s) => s !== estadoTarget.estado && estadoTarget.estado !== 'DADA_DE_BAJA')
    : []

  const columns = [
    { key: 'codigo', header: 'Código', className: 'font-medium' },
    { key: 'clienteNombre', header: 'Cliente' },
    { key: 'sectorNombre', header: 'Sector' },
    {
      key: 'fechaAlta',
      header: 'Fecha de alta',
      render: (t) => <span className="text-slate-600 dark:text-slate-300">{formatDate(t.fechaAlta)}</span>,
    },
    {
      key: 'estado',
      header: 'Estado',
      render: (t) => <Badge tone={estadoTone(t.estado)}>{etiquetaEstado[t.estado] || t.estado}</Badge>,
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (t) =>
        t.estado === 'DADA_DE_BAJA' ? (
          <span className="text-xs text-slate-400 dark:text-slate-500">Sin acciones</span>
        ) : (
          <div className="flex items-center gap-1">
            <Button variant="ghost" onClick={() => abrirEstado(t)} title="Cambiar estado">
              <Prohibit size={18} weight="duotone" />
            </Button>
            <Button variant="ghost" onClick={() => abrirBaja(t)} title="Dar de baja">
              <SignOut size={18} weight="duotone" />
            </Button>
          </div>
        ),
    },
  ]

  return (
    <div>
      <PageHeader title="Tomas" subtitle="Conexiones del servicio asociadas a un cliente y un sector">
        <Button onClick={abrirCrear}>
          <Plus size={16} weight="bold" />
          Nueva toma
        </Button>
      </PageHeader>

      <ErrorBanner>{error}</ErrorBanner>

      <DataTable
        columns={columns}
        data={tomas}
        keyField="id"
        loading={loading}
        emptyTitle="Sin tomas"
        emptyText="Aún no hay tomas registradas. Crea la primera con el botón «Nueva toma»."
      />

      <Modal
        open={formOpen}
        onClose={saving ? undefined : () => setFormOpen(false)}
        title="Nueva toma"
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="form-toma" disabled={saving}>
              {saving ? <Spinner size={14} /> : null}
              {saving ? 'Guardando…' : 'Crear toma'}
            </Button>
          </>
        }
      >
        <form id="form-toma" onSubmit={guardar} className="grid gap-4 sm:grid-cols-2">
          <Field label="Código" required>
            <input
              className={inputCls}
              value={form.codigo}
              onChange={(e) => setForm((f) => ({ ...f, codigo: e.target.value }))}
              placeholder="Código de la toma"
              maxLength={40}
              required
            />
          </Field>
          <Field label="Sector" required hint={sectores.length === 0 ? 'No hay sectores disponibles' : undefined}>
            <select
              className={inputCls}
              value={form.sectorId}
              onChange={(e) => setForm((f) => ({ ...f, sectorId: e.target.value }))}
              required
            >
              <option value="" disabled>
                Selecciona un sector
              </option>
              {sectores.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.nombre}
                </option>
              ))}
            </select>
          </Field>
          <Field
            label="Cliente"
            required
            hint={clientes.length === 0 ? 'No hay clientes disponibles' : undefined}
            className="sm:col-span-2"
          >
            <select
              className={inputCls}
              value={form.clienteId}
              onChange={(e) => setForm((f) => ({ ...f, clienteId: e.target.value }))}
              required
            >
              <option value="" disabled>
                Selecciona un cliente
              </option>
              {clientes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre} — {c.documento}
                </option>
              ))}
            </select>
          </Field>
          {formError && <p className="text-sm text-red-600 dark:text-red-400 sm:col-span-2">{formError}</p>}
        </form>
      </Modal>

      <Modal
        open={estadoOpen}
        onClose={cambiando ? undefined : () => setEstadoOpen(false)}
        title="Cambiar estado de la toma"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setEstadoOpen(false)} disabled={cambiando}>
              Cancelar
            </Button>
            <Button onClick={guardarEstado} disabled={cambiando || !nuevoEstado || nuevoEstado === estadoTarget?.estado}>
              {cambiando ? <Spinner size={14} /> : null}
              {cambiando ? 'Guardando…' : 'Guardar'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="flex items-center justify-between rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
            <div>
              <p className="text-sm font-medium">{estadoTarget?.codigo}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {estadoTarget?.clienteNombre} · {estadoTarget?.sectorNombre}
              </p>
            </div>
            <Badge tone={estadoTone(estadoTarget?.estado)}>{etiquetaEstado[estadoTarget?.estado] || estadoTarget?.estado}</Badge>
          </div>
          {opcionesEstado.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              La toma dada de baja no puede cambiar de estado.
            </p>
          ) : (
            <Field label="Nuevo estado">
              <select className={inputCls} value={nuevoEstado} onChange={(e) => setNuevoEstado(e.target.value)}>
                <option value="" disabled>
                  Selecciona un estado
                </option>
                {opcionesEstado.map((s) => (
                  <option key={s} value={s}>
                    {etiquetaEstado[s]}
                  </option>
                ))}
              </select>
            </Field>
          )}
          {estadoError && <p className="text-sm text-red-600 dark:text-red-400">{estadoError}</p>}
        </div>
      </Modal>

      <ConfirmDialog
        open={!!bajaTarget}
        onClose={() => setBajaTarget(null)}
        onConfirm={confirmarBaja}
        title="Dar de baja la toma"
        confirmLabel="Dar de baja"
        busy={dandoBaja}
        message={
          <span>
            ¿Seguro que deseas dar de baja la toma <span className="font-semibold">{bajaTarget?.codigo}</span>? Esta
            acción no se puede deshacer.
            {bajaError && <span className="mt-2 block text-red-600 dark:text-red-400">{bajaError}</span>}
          </span>
        }
      />
    </div>
  )
}