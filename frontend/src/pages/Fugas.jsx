import { useEffect, useState } from 'react'
import {
  ArrowRight,
  CheckCircle,
  PencilSimple,
  Plus,
  Trash,
  Warning,
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
  REPORTADA: 'Reportada',
  EN_CURSO: 'En curso',
  RESUELTA: 'Resuelta',
}

const SEVERIDAD_LABEL = {
  BAJA: 'Baja',
  MEDIA: 'Media',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
}

const SEVERIDAD_TONE = {
  BAJA: 'sky',
  MEDIA: 'amber',
  ALTA: 'red',
  CRITICA: 'red',
}

export default function Fugas() {
  const [fugas, setFugas] = useState([])
  const [sectores, setSectores] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(null)

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({})
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)

  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  const [actionTarget, setActionTarget] = useState(null)
  const [busy, setBusy] = useState(false)

  async function load() {
    setLoading(true)
    setLoadError(null)
    try {
      const [items, secs] = await Promise.all([api.get('/fugas'), api.get('/sectores')])
      setFugas(items)
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
    setForm({ sectorId: sectores[0]?.id ?? '', ubicacion: '', severidad: 'BAJA' })
    setSaveError(null)
    setFormOpen(true)
  }

  function openEdit(r) {
    setEditing(r)
    setForm({
      sectorId: r.sectorId,
      ubicacion: r.ubicacion,
      severidad: r.severidad,
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
    if (!form.ubicacion.trim()) {
      setSaveError('La ubicación es obligatoria')
      return
    }
    setSaving(true)
    setSaveError(null)
    try {
      const payload = {
        sectorId: Number(form.sectorId),
        ubicacion: form.ubicacion.trim(),
        severidad: form.severidad,
      }
      if (editing) {
        await api.put(`/fugas/${editing.id}`, payload)
      } else {
        await api.post('/fugas', payload)
      }
      setFormOpen(false)
      load()
    } catch (err) {
      setSaveError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await api.del(`/fugas/${deleteTarget.id}`)
      setDeleteTarget(null)
      load()
    } catch (err) {
      setLoadError(err.message)
      setDeleteTarget(null)
    } finally {
      setDeleting(false)
    }
  }

  async function handleAction() {
    if (!actionTarget) return
    setBusy(true)
    try {
      if (actionTarget.action === 'en-curso') {
        await api.put(`/fugas/${actionTarget.id}/en-curso`)
      } else if (actionTarget.action === 'resolver') {
        await api.put(`/fugas/${actionTarget.id}/resolver`)
      }
      setActionTarget(null)
      load()
    } catch (e) {
      setLoadError(e.message)
      setActionTarget(null)
    } finally {
      setBusy(false)
    }
  }

  const columns = [
    { key: 'id', header: 'ID', className: 'w-16' },
    { key: 'sectorNombre', header: 'Sector' },
    { key: 'ubicacion', header: 'Ubicación', className: 'max-w-xs truncate' },
    {
      key: 'severidad',
      header: 'Severidad',
      render: (r) => (
        <Badge tone={SEVERIDAD_TONE[r.severidad] || 'neutral'}>
          {SEVERIDAD_LABEL[r.severidad] || r.severidad}
        </Badge>
      ),
    },
    {
      key: 'estado',
      header: 'Estado',
      render: (r) => (
        <Badge tone={estadoTone(r.estado)}>{ESTADO_LABEL[r.estado] || r.estado}</Badge>
      ),
    },
    { key: 'fechaReporte', header: 'Reporte', render: (r) => formatDate(r.fechaReporte) },
    { key: 'fechaResolucion', header: 'Resolución', render: (r) => formatDate(r.fechaResolucion) },
    {
      key: 'acciones',
      header: 'Acciones',
      align: 'right',
      render: (r) => (
        <div className="flex justify-end gap-1">
          {r.estado === 'REPORTADA' && (
            <Button variant="ghost" className="!px-2.5" onClick={() => setActionTarget({ ...r, action: 'en-curso' })}>
              <ArrowRight size={16} weight="bold" />
              En curso
            </Button>
          )}
          {r.estado !== 'RESUELTA' && (
            <Button
              variant="ghost"
              className="!px-2.5 !text-emerald-600 hover:!bg-emerald-50 dark:!text-emerald-400 dark:hover:!bg-emerald-500/10"
              onClick={() => setActionTarget({ ...r, action: 'resolver' })}
            >
              <CheckCircle size={16} weight="bold" />
              Resolver
            </Button>
          )}
          {r.estado !== 'RESUELTA' && (
            <>
              <Button
                variant="ghost"
                className="!px-2.5"
                onClick={(e) => {
                  e.stopPropagation()
                  openEdit(r)
                }}
                aria-label={`Editar fuga ${r.id}`}
              >
                <PencilSimple size={16} weight="bold" />
              </Button>
              <Button
                variant="ghost"
                className="!px-2.5 !text-red-600 hover:!bg-red-50 dark:!text-red-400 dark:hover:!bg-red-500/10"
                onClick={(e) => {
                  e.stopPropagation()
                  setDeleteTarget(r)
                }}
                aria-label={`Eliminar fuga ${r.id}`}
              >
                <Trash size={16} weight="bold" />
              </Button>
            </>
          )}
        </div>
      ),
    },
  ]

  const actionMessage =
    actionTarget?.action === 'resolver'
      ? `¿Marcar como resuelta la fuga en "${actionTarget.ubicacion}"?`
      : `¿Mover a "En curso" la fuga en "${actionTarget?.ubicacion}"?`

  return (
    <div>
      <PageHeader title="Fugas" subtitle={`${fugas.length} fugas reportadas`}>
        <Button onClick={openCreate}>
          <Plus size={16} weight="bold" />
          Reportar fuga
        </Button>
      </PageHeader>

      {loadError ? (
        <div className="card">
          <EmptyState icon={Warning} title="No se pudieron cargar las fugas" text={loadError}>
            <Button variant="secondary" onClick={load}>
              Reintentar
            </Button>
          </EmptyState>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={fugas}
          loading={loading}
          emptyTitle="Sin fugas"
          emptyText="No hay fugas reportadas en la red."
        />
      )}

      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? 'Editar fuga' : 'Reportar fuga'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="fuga-form" disabled={saving}>
              {saving ? <Spinner size={14} /> : null}
              {saving ? 'Guardando…' : editing ? 'Guardar cambios' : 'Reportar fuga'}
            </Button>
          </>
        }
      >
        <form id="fuga-form" onSubmit={handleSave} className="space-y-4">
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
          <Field label="Ubicación" required>
            <input
              className={inputCls}
              value={form.ubicacion}
              onChange={(e) => setForm({ ...form, ubicacion: e.target.value })}
              placeholder="Ej. Av. Central, km 2"
              required
            />
          </Field>
          <Field label="Severidad" required>
            <select
              className={inputCls}
              value={form.severidad}
              onChange={(e) => setForm({ ...form, severidad: e.target.value })}
            >
              <option value="BAJA">Baja</option>
              <option value="MEDIA">Media</option>
              <option value="ALTA">Alta</option>
              <option value="CRITICA">Crítica</option>
            </select>
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        open={actionTarget !== null}
        onClose={() => setActionTarget(null)}
        onConfirm={handleAction}
        busy={busy}
        title={actionTarget?.action === 'resolver' ? 'Resolver fuga' : 'Fuga en curso'}
        confirmLabel={actionTarget?.action === 'resolver' ? 'Resolver' : 'En curso'}
        message={actionMessage}
      />

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        busy={deleting}
        title="Eliminar fuga"
        confirmLabel="Eliminar"
        message={
          deleteTarget
            ? `¿Eliminar la fuga en "${deleteTarget.ubicacion}"? Esta acción no se puede deshacer.`
            : ''
        }
      />
    </div>
  )
}
