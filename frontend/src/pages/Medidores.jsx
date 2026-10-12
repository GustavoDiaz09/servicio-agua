import { useEffect, useState } from 'react'
import { ArrowClockwise, Gauge, Plus, Prohibit, SignOut } from '@phosphor-icons/react'
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
  inputCls,
} from '../components/ui'

const ETIQUETA_ESTADO = {
  ACTIVO: 'Activo',
  INOPERANTE: 'Inoperante',
  RETIRADO: 'Retirado',
}

const TONE_ESTADO = {
  ACTIVO: 'green',
  INOPERANTE: 'amber',
  RETIRADO: 'red',
}

const ESTADOS_FILTRO = [
  { value: '', label: 'Todos los estados' },
  { value: 'ACTIVO', label: 'Activos' },
  { value: 'INOPERANTE', label: 'Inoperantes' },
  { value: 'RETIRADO', label: 'Retirados' },
]

const vacio = { numeroSerie: '', lecturaAnteriorAjuste: '' }

export default function Medidores() {
  const [medidores, setMedidores] = useState([])
  const [tomas, setTomas] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filtroEstado, setFiltroEstado] = useState('')

  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(vacio)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const [asignarTarget, setAsignarTarget] = useState(null)
  const [tomaId, setTomaId] = useState('')
  const [asignarError, setAsignarError] = useState('')
  const [asignando, setAsignando] = useState(false)

  const [retirarTarget, setRetirarTarget] = useState(null)
  const [retirarError, setRetirarError] = useState('')
  const [retirando, setRetirando] = useState(false)

  const [estadoTarget, setEstadoTarget] = useState(null)
  const [estadoOpen, setEstadoOpen] = useState(false)
  const [nuevoEstado, setNuevoEstado] = useState('')
  const [estadoError, setEstadoError] = useState('')
  const [cambiando, setCambiando] = useState(false)

  async function cargar() {
    setLoading(true)
    setError('')
    try {
      const ruta = filtroEstado ? `/medidores/estado/${filtroEstado}` : '/medidores'
      setMedidores(await api.get(ruta) || [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    cargar()
  }, [filtroEstado])

  async function cargarTomas() {
    try {
      setTomas(await api.get('/tomas') || [])
    } catch (e) {
      setTomas([])
      setAsignarError('No se pudieron cargar las tomas: ' + e.message)
    }
  }

  async function abrirCrear() {
    setForm(vacio)
    setFormError('')
    setFormOpen(true)
  }

  async function guardar(e) {
    e.preventDefault()
    if (!form.numeroSerie.trim()) {
      setFormError('El número de serie es obligatorio')
      return
    }
    setSaving(true)
    setFormError('')
    try {
      await api.post('/medidores', {
        numeroSerie: form.numeroSerie.trim(),
        lecturaAnteriorAjuste:
          form.lecturaAnteriorAjuste === '' ? null : Number(form.lecturaAnteriorAjuste),
      })
      setFormOpen(false)
      await cargar()
    } catch (e) {
      setFormError(e.message)
    } finally {
      setSaving(false)
    }
  }

  async function abrirAsignar(m) {
    setAsignarTarget(m)
    setTomaId('')
    setAsignarError('')
    await cargarTomas()
  }

  async function confirmarAsignacion() {
    if (!tomaId) {
      setAsignarError('Seleccioná una toma')
      return
    }
    setAsignando(true)
    setAsignarError('')
    try {
      await api.put(`/medidores/${asignarTarget.id}/asignar`, { tomaId: Number(tomaId) })
      setAsignarTarget(null)
      await cargar()
    } catch (e) {
      setAsignarError(e.message)
    } finally {
      setAsignando(false)
    }
  }

  async function confirmarRetiro() {
    setRetirando(true)
    setRetirarError('')
    try {
      await api.put(`/medidores/${retirarTarget.id}/retirar`)
      setRetirarTarget(null)
      await cargar()
    } catch (e) {
      setRetirarError(e.message)
    } finally {
      setRetirando(false)
    }
  }

  function abrirEstado(m) {
    setEstadoTarget(m)
    setNuevoEstado(m.estado === 'ACTIVO' ? 'INOPERANTE' : 'ACTIVO')
    setEstadoError('')
    setEstadoOpen(true)
  }

  async function guardarEstado() {
    setCambiando(true)
    setEstadoError('')
    try {
      await api.put(`/medidores/${estadoTarget.id}/estado`, { estado: nuevoEstado })
      setEstadoOpen(false)
      await cargar()
    } catch (e) {
      setEstadoError(e.message)
    } finally {
      setCambiando(false)
    }
  }

  const total = medidores.length
  const activos = medidores.filter((m) => m.estado === 'ACTIVO').length
  const retirados = medidores.filter((m) => m.estado === 'RETIRADO').length

  const columns = [
    {
      key: 'numeroSerie',
      header: 'Número de serie',
      render: (m) => <span className="font-medium text-slate-900 dark:text-slate-100">{m.numeroSerie}</span>,
    },
    {
      key: 'toma',
      header: 'Toma',
      render: (m) => (
        <span className="text-slate-600 dark:text-slate-300">{m.tomaCodigo || 'Sin asignar'}</span>
      ),
    },
    {
      key: 'lecturaAnteriorAjuste',
      header: 'Ajuste lectura',
      align: 'right',
      render: (m) => (
        <span className="num text-slate-600 dark:text-slate-300">
          {m.lecturaAnteriorAjuste ?? '—'}
        </span>
      ),
    },
    {
      key: 'fechaInstalacion',
      header: 'Instalación',
      render: (m) => <span className="text-slate-600 dark:text-slate-300">{formatDate(m.fechaInstalacion)}</span>,
    },
    {
      key: 'estado',
      header: 'Estado',
      render: (m) => (
        <Badge tone={TONE_ESTADO[m.estado]}>{ETIQUETA_ESTADO[m.estado] || m.estado}</Badge>
      ),
    },
    {
      key: 'acciones',
      header: 'Acciones',
      align: 'right',
      render: (m) =>
        m.estado === 'RETIRADO' ? (
          <span className="text-xs text-slate-400 dark:text-slate-500">Sin acciones</span>
        ) : (
          <div className="flex justify-end gap-1">
            <Button
              variant="ghost"
              className="!px-2.5"
              onClick={() => abrirAsignar(m)}
              title={m.tomaCodigo ? 'Cambiar de toma' : 'Asignar a una toma'}
            >
              <ArrowClockwise size={16} weight="bold" />
              Asignar
            </Button>
            {m.tomaCodigo && (
              <Button
                variant="ghost"
                className="!px-2.5 !text-red-600 hover:!bg-red-50 dark:!text-red-400 dark:hover:!bg-red-500/10"
                onClick={() => setRetirarTarget(m)}
                title="Retirar medidor"
              >
                <SignOut size={16} weight="bold" />
                Retirar
              </Button>
            )}
            <Button
              variant="ghost"
              className="!px-2.5"
              onClick={() => abrirEstado(m)}
              title="Cambiar estado"
            >
              <Prohibit size={16} weight="bold" />
              Estado
            </Button>
          </div>
        ),
    },
  ]

  return (
    <div>
      <PageHeader title="Medidores" subtitle="Dispositivos de medición asignados a las tomas">
        <Button onClick={abrirCrear}>
          <Plus size={16} weight="bold" />
          Nuevo medidor
        </Button>
      </PageHeader>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select className={inputCls.replace('w-full', 'w-56')} value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)}>
          {ESTADOS_FILTRO.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
        <p className="num text-sm text-slate-500 dark:text-slate-400">
          {loading ? '…' : `${total} medidor(es) · ${activos} activo(s) · ${retirados} retirado(s)`}
        </p>
      </div>

      {error && (
        <div className="card mb-4 rounded-lg border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">
          {error}
        </div>
      )}

      <DataTable
        columns={columns}
        data={medidores}
        keyField="id"
        loading={loading}
        emptyTitle="Sin medidores"
        emptyText={
          filtroEstado
            ? `No hay medidores en estado «${ETIQUETA_ESTADO[filtroEstado]}».`
            : 'Aún no hay medidores registrados. Crea el primero con el botón «Nuevo medidor».'
        }
      />

      <Modal
        open={formOpen}
        onClose={saving ? undefined : () => setFormOpen(false)}
        title="Nuevo medidor"
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="form-medidor" disabled={saving}>
              {saving ? <Spinner size={14} /> : null}
              {saving ? 'Guardando…' : 'Crear medidor'}
            </Button>
          </>
        }
      >
        <form id="form-medidor" onSubmit={guardar} className="space-y-4">
          {formError && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-400">
              {formError}
            </p>
          )}
          <Field label="Número de serie" required>
            <input
              className={inputCls}
              value={form.numeroSerie}
              onChange={(e) => setForm({ ...form, numeroSerie: e.target.value })}
              placeholder="Ej. MTR-2001"
              maxLength={50}
              required
            />
          </Field>
          <Field
            label="Lectura anterior de ajuste"
            hint="Opcional. Valor base para el cálculo de consumo."
          >
            <input
              type="number"
              min="0"
              step="1"
              className={inputCls}
              value={form.lecturaAnteriorAjuste}
              onChange={(e) => setForm({ ...form, lecturaAnteriorAjuste: e.target.value })}
              placeholder="Ej. 0"
            />
          </Field>
        </form>
      </Modal>

      <Modal
        open={asignarTarget !== null}
        onClose={asignando ? undefined : () => setAsignarTarget(null)}
        title={`Asignar medidor ${asignarTarget?.numeroSerie || ''}`}
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setAsignarTarget(null)} disabled={asignando}>
              Cancelar
            </Button>
            <Button onClick={confirmarAsignacion} disabled={asignando}>
              {asignando ? <Spinner size={14} /> : null}
              {asignando ? 'Asignando…' : 'Asignar'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <span className="rounded-full bg-accent-50 p-2 text-accent-600 dark:bg-accent-500/10 dark:text-accent-300">
              <Gauge size={20} weight="duotone" />
            </span>
            <div>
              <p className="text-sm font-medium">{asignarTarget?.numeroSerie}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {asignarTarget?.tomaCodigo ? `Actualmente en ${asignarTarget.tomaCodigo}` : 'Sin asignar'}
              </p>
            </div>
          </div>
          <Field label="Toma" required hint={tomas.length === 0 ? 'No hay tomas activas disponibles' : undefined}>
            <select
              className={inputCls}
              value={tomaId}
              onChange={(e) => setTomaId(e.target.value)}
              required
            >
              <option value="" disabled>
                Selecciona una toma
              </option>
              {tomas
                .filter((t) => t.estado === 'ACTIVA')
                .map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.codigo} — {t.clienteNombre}
                  </option>
                ))}
            </select>
          </Field>
          {asignarError && <p className="text-sm text-red-600 dark:text-red-400">{asignarError}</p>}
        </div>
      </Modal>

      <Modal
        open={estadoOpen}
        onClose={cambiando ? undefined : () => setEstadoOpen(false)}
        title="Cambiar estado del medidor"
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
              <Gauge size={20} weight="duotone" />
            </span>
            <div>
              <p className="text-sm font-medium">{estadoTarget?.numeroSerie}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">{estadoTarget?.tomaCodigo || 'Sin asignar'}</p>
            </div>
          </div>
          <Field label="Nuevo estado">
            <select className={inputCls} value={nuevoEstado} onChange={(e) => setNuevoEstado(e.target.value)}>
              <option value="ACTIVO">Activo</option>
              <option value="INOPERANTE">Inoperante</option>
            </select>
          </Field>
          {estadoError && <p className="text-sm text-red-600 dark:text-red-400">{estadoError}</p>}
        </div>
      </Modal>

      <ConfirmDialog
        open={retirarTarget !== null}
        onClose={() => setRetirarTarget(null)}
        onConfirm={confirmarRetiro}
        busy={retirando}
        title="Retirar medidor"
        confirmLabel="Retirar"
        message={
          <span>
            ¿Retirar el medidor <span className="font-semibold">{retirarTarget?.numeroSerie}</span> de la toma{' '}
            <span className="font-semibold">{retirarTarget?.tomaCodigo}</span>? Quedará marcado como retirado.
            {retirarError && <span className="mt-2 block text-red-600 dark:text-red-400">{retirarError}</span>}
          </span>
        }
      />
    </div>
  )
}