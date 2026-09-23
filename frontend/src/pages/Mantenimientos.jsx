import { useEffect, useState } from 'react'
import {
  ArrowRight,
  CheckCircle,
  Plus,
  Wrench,
  XCircle,
} from '@phosphor-icons/react'
import { api } from '../api/client'
import { formatDate } from '../lib/format'
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

const ESTADO_LABEL = {
  PROGRAMADO: 'Programado',
  EN_CURSO: 'En curso',
  COMPLETADO: 'Completado',
  CANCELADO: 'Cancelado',
}

const TIPO_TONE = {
  PREVENTIVO: 'sky',
  CORRECTIVO: 'red',
}

export default function Mantenimientos() {
  const [mantenimientos, setMantenimientos] = useState([])
  const [sectores, setSectores] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(null)

  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState({})
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)

  const [actionTarget, setActionTarget] = useState(null)
  const [busy, setBusy] = useState(false)

  async function load() {
    setLoading(true)
    setLoadError(null)
    try {
      const [items, secs] = await Promise.all([api.get('/mantenimientos'), api.get('/sectores')])
      setMantenimientos(items)
      setSectores(secs)
    } catch (e) {
      setLoadError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  function openCreate() {
    setForm({ sectorId: sectores[0]?.id ?? '', tipo: 'PREVENTIVO', descripcion: '', fechaProgramada: '' })
    setSaveError(null)
    setFormOpen(true)
  }

  async function handleSave(e) {
    e.preventDefault()
    if (!form.sectorId) {
      setSaveError('Selecciona un sector')
      return
    }
    if (!form.descripcion.trim()) {
      setSaveError('La descripción es obligatoria')
      return
    }
    if (!form.fechaProgramada) {
      setSaveError('La fecha programada es obligatoria')
      return
    }
    setSaving(true)
    setSaveError(null)
    try {
      await api.post('/mantenimientos', {
        sectorId: Number(form.sectorId),
        descripcion: form.descripcion.trim(),
        tipo: form.tipo,
        fechaProgramada: form.fechaProgramada,
      })
      setFormOpen(false)
      load()
    } catch (err) {
      setSaveError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleAction() {
    if (!actionTarget) return
    setBusy(true)
    try {
      await api.put(`/mantenimientos/${actionTarget.id}/estado`, {
        estado: actionTarget.next,
      })
      setActionTarget(null)
      load()
    } catch (e) {
      setLoadError(e.message)
      setActionTarget(null)
    } finally {
      setBusy(false)
    }
  }

  const today = new Date().toISOString().slice(0, 10)

  const columns = [
    { key: 'id', header: 'ID', className: 'w-16' },
    { key: 'sectorNombre', header: 'Sector' },
    {
      key: 'tipo',
      header: 'Tipo',
      render: (r) => (
        <Badge tone={TIPO_TONE[r.tipo] || 'neutral'}>
          {r.tipo === 'PREVENTIVO' ? 'Preventivo' : r.tipo === 'CORRECTIVO' ? 'Correctivo' : r.tipo}
        </Badge>
      ),
    },
    { key: 'descripcion', header: 'Descripción', className: 'max-w-xs truncate' },
    { key: 'fechaProgramada', header: 'Programado', render: (r) => formatDate(r.fechaProgramada) },
    { key: 'fechaFin', header: 'Realizado', render: (r) => formatDate(r.fechaFin) },
    {
      key: 'estado',
      header: 'Estado',
      render: (r) => (
        <Badge tone={estadoTone(r.estado)}>{ESTADO_LABEL[r.estado] || r.estado}</Badge>
      ),
    },
    {
      key: 'acciones',
      header: 'Acciones',
      align: 'right',
      render: (r) => (
        <div className="flex justify-end gap-1">
          {r.estado === 'PROGRAMADO' && (
            <>
              <Button variant="ghost" className="!px-2.5" onClick={() => setActionTarget({ ...r, next: 'EN_CURSO' })}>
                <ArrowRight size={16} weight="bold" />
                Iniciar
              </Button>
              <Button
                variant="ghost"
                className="!px-2.5 !text-red-600 hover:!bg-red-50 dark:!text-red-400 dark:hover:!bg-red-500/10"
                onClick={() => setActionTarget({ ...r, next: 'CANCELADO' })}
              >
                <XCircle size={16} weight="bold" />
                Cancelar
              </Button>
            </>
          )}
          {r.estado === 'EN_CURSO' && (
            <Button
              variant="ghost"
              className="!px-2.5 !text-emerald-600 hover:!bg-emerald-50 dark:!text-emerald-400 dark:hover:!bg-emerald-500/10"
              onClick={() => setActionTarget({ ...r, next: 'COMPLETADO' })}
            >
              <CheckCircle size={16} weight="bold" />
              Completar
            </Button>
          )}
        </div>
      ),
    },
  ]

  const actionLabel =
    actionTarget?.next === 'EN_CURSO'
      ? 'Iniciar mantenimiento'
      : actionTarget?.next === 'COMPLETADO'
        ? 'Completar mantenimiento'
        : 'Cancelar mantenimiento'

  const actionMessage =
    actionTarget?.next === 'EN_CURSO'
      ? `¿Iniciar el mantenimiento "${actionTarget.descripcion}" en el sector "${actionTarget.sectorNombre}"?`
      : actionTarget?.next === 'COMPLETADO'
        ? `¿Marcar como completado el mantenimiento "${actionTarget.descripcion}"?`
        : `¿Cancelar el mantenimiento "${actionTarget?.descripcion}"? Esta acción no se puede deshacer.`

  return (
    <div>
      <PageHeader title="Mantenimientos" subtitle={`${mantenimientos.length} mantenimientos registrados`}>
        <Button onClick={openCreate}>
          <Plus size={16} weight="bold" />
          Nuevo mantenimiento
        </Button>
      </PageHeader>

      {loadError ? (
        <div className="card">
          <EmptyState icon={Wrench} title="No se pudieron cargar los mantenimientos" text={loadError}>
            <Button variant="secondary" onClick={load}>
              Reintentar
            </Button>
          </EmptyState>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={mantenimientos}
          loading={loading}
          emptyTitle="Sin mantenimientos"
          emptyText="Programa el primer mantenimiento de la red."
        />
      )}

      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title="Nuevo mantenimiento"
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="mantenimiento-form" disabled={saving}>
              {saving ? <Spinner size={14} /> : null}
              {saving ? 'Guardando…' : 'Crear mantenimiento'}
            </Button>
          </>
        }
      >
        <form id="mantenimiento-form" onSubmit={handleSave} className="space-y-4">
          {saveError && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-400">
              {saveError}
            </p>
          )}
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Sector" required>
              <select
                className={inputCls}
                value={form.sectorId}
                onChange={(e) => setForm({ ...form, sectorId: e.target.value })}
                required
              >
                <option value="">Seleccionar sector</option>
                {sectores.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nombre}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Tipo" required>
              <select
                className={inputCls}
                value={form.tipo}
                onChange={(e) => setForm({ ...form, tipo: e.target.value })}
              >
                <option value="PREVENTIVO">Preventivo</option>
                <option value="CORRECTIVO">Correctivo</option>
              </select>
            </Field>
          </div>
          <Field label="Descripción" required>
            <input
              className={inputCls}
              value={form.descripcion}
              onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
              placeholder="Ej. Revisión de válvulas de sector"
              required
            />
          </Field>
          <Field label="Fecha programada" required>
            <input
              type="date"
              min={today}
              className={inputCls}
              value={form.fechaProgramada}
              onChange={(e) => setForm({ ...form, fechaProgramada: e.target.value })}
              required
            />
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        open={actionTarget !== null}
        onClose={() => setActionTarget(null)}
        onConfirm={handleAction}
        busy={busy}
        title={actionLabel}
        confirmLabel={actionLabel}
        message={actionMessage}
      />
    </div>
  )
}