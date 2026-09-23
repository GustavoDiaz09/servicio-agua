import { useEffect, useState } from 'react'
import {
  ArrowRight,
  CheckCircle,
  PencilSimple,
  Plus,
  Trash,
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
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({})
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)

  const [actionTarget, setActionTarget] = useState(null)
  const [busy, setBusy] = useState(false)

  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

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
    setEditing(null)
    setForm({ sectorId: sectores[0]?.id ?? '', tipo: 'PREVENTIVO', descripcion: '', fechaProgramada: '' })
    setSaveError(null)
    setFormOpen(true)
  }

  function openEdit(r) {
    setEditing(r)
    setForm({
      sectorId: r.sectorId ?? '',
      tipo: r.tipo || 'PREVENTIVO',
      descripcion: r.descripcion || '',
      fechaProgramada: r.fechaProgramada || '',
    })
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
      const payload = {
        sectorId: Number(form.sectorId),
        descripcion: form.descripcion.trim(),
        tipo: form.tipo,
        fechaProgramada: form.fechaProgramada,
      }
      if (editing) {
        await api.put(`/mantenimientos/${editing.id}`, payload)
      } else {
        await api.post('/mantenimientos', payload)
      }
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

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await api.delete(`/mantenimientos/${deleteTarget.id}`)
      setDeleteTarget(null)
      load()
    } catch (e) {
      setLoadError(e.message)
      setDeleteTarget(null)
    } finally {
      setDeleting(false)
    }
  }

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
          <Button variant="ghost" className="!px-2.5" onClick={() => openEdit(r)} aria-label={`Editar mantenimiento ${r.id}`}>
            <PencilSimple size={16} weight="bold" />
          </Button>
          <Button
            variant="ghost"
            className="!px-2.5 !text-red-600 hover:!bg-red-50 dark:!text-red-400 dark:hover:!bg-red-500/10"
            onClick={() => setDeleteTarget(r)}
            aria-label={`Eliminar mantenimiento ${r.id}`}
          >
            <Trash size={16} weight="bold" />
          </Button>
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
      ? 'Iniciar'
      : actionTarget?.next === 'COMPLETADO'
        ? 'Completar'
        : 'Cancelar'

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
        title={editing ? 'Editar mantenimiento' : 'Nuevo mantenimiento'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="mantenimiento-form" disabled={saving}>
              {saving ? <Spinner size={14} /> : null}
              {saving ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear mantenimiento'}
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
              min={new Date().toISOString().slice(0, 10)}
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
        message={
          actionTarget?.next === 'EN_CURSO'
            ? `¿Iniciar el mantenimiento "${actionTarget.descripcion}"?`
            : actionTarget?.next === 'COMPLETADO'
              ? `¿Marcar como completado el mantenimiento "${actionTarget.descripcion}"?`
              : `¿Cancelar el mantenimiento "${actionTarget?.descripcion}"? Esta acción no se puede deshacer.`
        }
      />

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        busy={deleting}
        title="Eliminar mantenimiento"
        confirmLabel="Eliminar"
        message={`¿Eliminar el mantenimiento "${deleteTarget?.descripcion}"? Esta acción no se puede deshacer.`}
      />
    </div>
  )
}
