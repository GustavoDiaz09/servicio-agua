import { useCallback, useEffect, useState } from 'react'
import { PencilSimpleLine, Plus, Prohibit, UsersThree } from '@phosphor-icons/react'
import { api } from '../api/client'
import {
  Badge,
  Button,
  DataTable,
  Field,
  Modal,
  PageHeader,
  Spinner,
  estadoTone,
  inputCls,
} from '../components/ui'

const TIPOS = ['RESIDENCIAL', 'COMERCIAL', 'INSTITUCIONAL', 'INDUSTRIAL']
const ESTADOS = ['ACTIVO', 'SUSPENDIDO', 'INACTIVO']

const etiquetaTipo = {
  RESIDENCIAL: 'Residencial',
  COMERCIAL: 'Comercial',
  INSTITUCIONAL: 'Institucional',
  INDUSTRIAL: 'Industrial',
}

const etiquetaEstado = {
  ACTIVO: 'Activo',
  SUSPENDIDO: 'Suspendido',
  INACTIVO: 'Inactivo',
}

const formVacio = { documento: '', nombre: '', telefono: '', email: '', direccion: '', tipo: '' }

function ErrorBanner({ children }) {
  if (!children) return null
  return (
    <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">
      {children}
    </div>
  )
}

export default function Clientes() {
  const [clientes, setClientes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filtroTipo, setFiltroTipo] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('')

  const [formOpen, setFormOpen] = useState(false)
  const [editId, setEditId] = useState(null)
  const [form, setForm] = useState(formVacio)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const [estadoOpen, setEstadoOpen] = useState(false)
  const [estadoTarget, setEstadoTarget] = useState(null)
  const [nuevoEstado, setNuevoEstado] = useState('')
  const [estadoError, setEstadoError] = useState('')
  const [cambiando, setCambiando] = useState(false)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = []
      if (filtroTipo) params.push(`tipo=${filtroTipo}`)
      if (filtroEstado) params.push(`estado=${filtroEstado}`)
      const res = await api.get(`/clientes${params.length ? '?' + params.join('&') : ''}`)
      setClientes(res || [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [filtroTipo, filtroEstado])

  useEffect(() => {
    cargar()
  }, [cargar])

  function abrirCrear() {
    setEditId(null)
    setForm(formVacio)
    setFormError('')
    setFormOpen(true)
  }

  function abrirEditar(c) {
    setEditId(c.id)
    setForm({
      documento: c.documento || '',
      nombre: c.nombre || '',
      telefono: c.telefono || '',
      email: c.email || '',
      direccion: c.direccion || '',
      tipo: c.tipo || '',
    })
    setFormError('')
    setFormOpen(true)
  }

  function setCampo(campo, valor) {
    setForm((f) => ({ ...f, [campo]: valor }))
  }

  async function guardar(e) {
    e.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      const body = {
        documento: form.documento,
        nombre: form.nombre,
        telefono: form.telefono || null,
        email: form.email || null,
        direccion: form.direccion || null,
        tipo: form.tipo,
      }
      if (editId) await api.put(`/clientes/${editId}`, body)
      else await api.post('/clientes', body)
      setFormOpen(false)
      await cargar()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSaving(false)
    }
  }

  function abrirEstado(c) {
    setEstadoTarget(c)
    setNuevoEstado(c.estado)
    setEstadoError('')
    setEstadoOpen(true)
  }

  async function guardarEstado() {
    setCambiando(true)
    setEstadoError('')
    try {
      await api.put(`/clientes/${estadoTarget.id}/estado`, { estado: nuevoEstado })
      setEstadoOpen(false)
      await cargar()
    } catch (err) {
      setEstadoError(err.message)
    } finally {
      setCambiando(false)
    }
  }

  const selectCls = inputCls.replace('w-full', 'w-48')

  const columns = [
    { key: 'documento', header: 'Documento', className: 'font-medium' },
    { key: 'nombre', header: 'Nombre' },
    {
      key: 'contacto',
      header: 'Contacto',
      render: (c) => (
        <div className="space-y-0.5">
          <p className="text-slate-900 dark:text-slate-100">{c.email || '—'}</p>
          <p className="text-xs text-slate-500 dark:text-slate-400">{c.telefono || '—'}</p>
        </div>
      ),
    },
    {
      key: 'tipo',
      header: 'Tipo',
      render: (c) => <span className="text-slate-600 dark:text-slate-300">{etiquetaTipo[c.tipo] || c.tipo}</span>,
    },
    {
      key: 'estado',
      header: 'Estado',
      render: (c) => <Badge tone={estadoTone(c.estado)}>{etiquetaEstado[c.estado] || c.estado}</Badge>,
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (c) => (
        <div className="flex items-center gap-1">
          <Button variant="ghost" onClick={() => abrirEditar(c)} title="Editar cliente">
            <PencilSimpleLine size={18} weight="duotone" />
          </Button>
          <Button variant="ghost" onClick={() => abrirEstado(c)} title="Cambiar estado">
            <Prohibit size={18} weight="duotone" />
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader title="Clientes" subtitle="Administra los clientes del servicio de agua potable">
        <Button onClick={abrirCrear}>
          <Plus size={16} weight="bold" />
          Nuevo cliente
        </Button>
      </PageHeader>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select className={selectCls} value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value)}>
          <option value="">Todos los tipos</option>
          {TIPOS.map((t) => (
            <option key={t} value={t}>
              {etiquetaTipo[t]}
            </option>
          ))}
        </select>
        <select className={selectCls} value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)}>
          <option value="">Todos los estados</option>
          {ESTADOS.map((s) => (
            <option key={s} value={s}>
              {etiquetaEstado[s]}
            </option>
          ))}
        </select>
        {(filtroTipo || filtroEstado) && (
          <Button
            variant="ghost"
            onClick={() => {
              setFiltroTipo('')
              setFiltroEstado('')
            }}
          >
            Limpiar filtros
          </Button>
        )}
      </div>

      <ErrorBanner>{error}</ErrorBanner>

      <DataTable
        columns={columns}
        data={clientes}
        keyField="id"
        loading={loading}
        emptyTitle="Sin clientes"
        emptyText="Aún no hay clientes registrados. Crea el primero con el botón «Nuevo cliente»."
      />

      <Modal
        open={formOpen}
        onClose={saving ? undefined : () => setFormOpen(false)}
        title={editId ? 'Editar cliente' : 'Nuevo cliente'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="form-cliente" disabled={saving}>
              {saving ? <Spinner size={14} /> : null}
              {saving ? 'Guardando…' : editId ? 'Guardar cambios' : 'Crear cliente'}
            </Button>
          </>
        }
      >
        <form id="form-cliente" onSubmit={guardar} className="grid gap-4 sm:grid-cols-2">
          <Field label="Documento" required error={formError && !form.documento ? formError : undefined}>
            <input
              className={inputCls}
              value={form.documento}
              onChange={(e) => setCampo('documento', e.target.value)}
              placeholder="N.º de documento"
              maxLength={30}
              required
            />
          </Field>
          <Field label="Nombre" required>
            <input
              className={inputCls}
              value={form.nombre}
              onChange={(e) => setCampo('nombre', e.target.value)}
              placeholder="Nombre completo"
              maxLength={120}
              required
            />
          </Field>
          <Field label="Teléfono">
            <input
              className={inputCls}
              value={form.telefono}
              onChange={(e) => setCampo('telefono', e.target.value)}
              placeholder="N.º de teléfono"
              maxLength={30}
            />
          </Field>
          <Field label="Email">
            <input
              type="email"
              className={inputCls}
              value={form.email}
              onChange={(e) => setCampo('email', e.target.value)}
              placeholder="correo@ejemplo.com"
              maxLength={120}
            />
          </Field>
          <Field label="Tipo de cliente" required className="sm:col-span-2">
            <select className={inputCls} value={form.tipo} onChange={(e) => setCampo('tipo', e.target.value)} required>
              <option value="" disabled>
                Selecciona un tipo
              </option>
              {TIPOS.map((t) => (
                <option key={t} value={t}>
                  {etiquetaTipo[t]}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Dirección" className="sm:col-span-2">
            <input
              className={inputCls}
              value={form.direccion}
              onChange={(e) => setCampo('direccion', e.target.value)}
              placeholder="Dirección de domicilio"
              maxLength={200}
            />
          </Field>
          {formError && (
            <p className="text-sm text-red-600 dark:text-red-400 sm:col-span-2">{formError}</p>
          )}
        </form>
      </Modal>

      <Modal
        open={estadoOpen}
        onClose={cambiando ? undefined : () => setEstadoOpen(false)}
        title="Cambiar estado del cliente"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setEstadoOpen(false)} disabled={cambiando}>
              Cancelar
            </Button>
            <Button onClick={guardarEstado} disabled={cambiando || nuevoEstado === estadoTarget?.estado}>
              {cambiando ? <Spinner size={14} /> : null}
              {cambiando ? 'Guardando…' : 'Guardar'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <span className="rounded-full bg-accent-50 p-2 text-accent-600 dark:bg-accent-500/10 dark:text-accent-300">
              <UsersThree size={20} weight="duotone" />
            </span>
            <div>
              <p className="text-sm font-medium">{estadoTarget?.nombre}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">{estadoTarget?.documento}</p>
            </div>
          </div>
          <Field label="Estado">
            <select className={inputCls} value={nuevoEstado} onChange={(e) => setNuevoEstado(e.target.value)}>
              {ESTADOS.map((s) => (
                <option key={s} value={s}>
                  {etiquetaEstado[s]}
                </option>
              ))}
            </select>
          </Field>
          {estadoError && <p className="text-sm text-red-600 dark:text-red-400">{estadoError}</p>}
        </div>
      </Modal>
    </div>
  )
}