import { useEffect, useState } from 'react'
import { MapTrifold, PencilSimple, Plus, Trash } from '@phosphor-icons/react'
import { api } from '../api/client'
import { formatNumber } from '../lib/format'
import {
  Button,
  ConfirmDialog,
  DataTable,
  EmptyState,
  Field,
  Modal,
  PageHeader,
  Spinner,
  inputCls,
} from '../components/ui'

const emptyForm = { nombre: '', zona: '', presionPromedio: '' }

export default function Sectores() {
  const [sectores, setSectores] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(null)

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)

  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  async function load() {
    setLoading(true)
    setLoadError(null)
    try {
      setSectores(await api.get('/sectores'))
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
    setForm(emptyForm)
    setSaveError(null)
    setFormOpen(true)
  }

  function openEdit(row) {
    setEditing(row)
    setForm({
      nombre: row.nombre || '',
      zona: row.zona || '',
      presionPromedio: row.presionPromedio ?? '',
    })
    setSaveError(null)
    setFormOpen(true)
  }

  async function handleSave(e) {
    e.preventDefault()
    if (!form.nombre.trim()) {
      setSaveError('El nombre es obligatorio')
      return
    }
    setSaving(true)
    setSaveError(null)
    const payload = {
      nombre: form.nombre.trim(),
      zona: form.zona.trim() || null,
      presionPromedio: form.presionPromedio === '' ? null : Number(form.presionPromedio),
    }
    try {
      if (editing) {
        await api.put(`/sectores/${editing.id}`, payload)
      } else {
        await api.post('/sectores', payload)
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
      await api.del(`/sectores/${deleteTarget.id}`)
      setDeleteTarget(null)
      load()
    } catch (e) {
      setLoadError(e.message)
      setDeleteTarget(null)
    } finally {
      setDeleting(false)
    }
  }

  const presion = (v) => (v === null || v === undefined ? '—' : `${formatNumber(v)} bar`)

  const columns = [
    { key: 'id', header: 'ID', className: 'w-16' },
    { key: 'nombre', header: 'Nombre' },
    { key: 'zona', header: 'Zona' },
    {
      key: 'presionPromedio',
      header: 'Presión promedio',
      align: 'right',
      render: (r) => <span className="num">{presion(r.presionPromedio)}</span>,
    },
    {
      key: 'acciones',
      header: 'Acciones',
      align: 'right',
      render: (r) => (
        <div className="flex justify-end gap-1">
          <Button
            variant="ghost"
            className="!px-2.5"
            onClick={(e) => {
              e.stopPropagation()
              openEdit(r)
            }}
            aria-label={`Editar ${r.nombre}`}
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
            aria-label={`Eliminar ${r.nombre}`}
          >
            <Trash size={16} weight="bold" />
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Sectores"
        subtitle={`${sectores.length} sectores registrados`}
      >
        <Button onClick={openCreate}>
          <Plus size={16} weight="bold" />
          Nuevo sector
        </Button>
      </PageHeader>

      {loadError ? (
        <div className="card">
          <EmptyState
            icon={MapTrifold}
            title="No se pudieron cargar los sectores"
            text={loadError}
          >
            <Button variant="secondary" onClick={load}>
              Reintentar
            </Button>
          </EmptyState>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={sectores}
          loading={loading}
          emptyTitle="Sin sectores"
          emptyText="Crea el primer sector para comenzar a organizar la red."
        />
      )}

      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? 'Editar sector' : 'Nuevo sector'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="sector-form" disabled={saving}>
              {saving ? <Spinner size={14} /> : null}
              {saving ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear sector'}
            </Button>
          </>
        }
      >
        <form id="sector-form" onSubmit={handleSave} className="space-y-4">
          {saveError && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-400">
              {saveError}
            </p>
          )}
          <Field label="Nombre" required error={undefined}>
            <input
              className={inputCls}
              value={form.nombre}
              onChange={(e) => setForm({ ...form, nombre: e.target.value })}
              placeholder="Ej. Sector Norte"
              required
            />
          </Field>
          <Field label="Zona">
            <input
              className={inputCls}
              value={form.zona}
              onChange={(e) => setForm({ ...form, zona: e.target.value })}
              placeholder="Ej. Centro"
            />
          </Field>
          <Field label="Presión promedio (bar)" hint="Opcional. En bar.">
            <input
              type="number"
              step="0.01"
              min="0"
              className={inputCls}
              value={form.presionPromedio}
              onChange={(e) => setForm({ ...form, presionPromedio: e.target.value })}
              placeholder="Ej. 45.5"
            />
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        busy={deleting}
        title="Eliminar sector"
        confirmLabel="Eliminar"
        message={`¿Eliminar el sector "${deleteTarget?.nombre}"? Esta acción no se puede deshacer.`}
      />
    </div>
  )
}