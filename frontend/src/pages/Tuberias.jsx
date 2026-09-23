import { useEffect, useState } from 'react'
import { FlowArrow, PencilSimple, Plus, Trash } from '@phosphor-icons/react'
import { api } from '../api/client'
import { formatDate, formatNumber } from '../lib/format'
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

export default function Tuberias() {
  const [tuberias, setTuberias] = useState([])
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

  async function load() {
    setLoading(true)
    setLoadError(null)
    try {
      const [tubs, secs] = await Promise.all([api.get('/tuberias'), api.get('/sectores')])
      setTuberias(tubs)
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
    setForm({ sectorId: sectores[0]?.id ?? '', material: '', diametroMm: '', longitudM: '', tramo: '', fechaInstalacion: '' })
    setSaveError(null)
    setFormOpen(true)
  }

  function openEdit(row) {
    setEditing(row)
    setForm({
      sectorId: row.sectorId,
      material: row.material || '',
      diametroMm: row.diametroMm ?? '',
      longitudM: row.longitudM ?? '',
      tramo: row.tramo || '',
      fechaInstalacion: row.fechaInstalacion || '',
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
    if (!form.material.trim()) {
      setSaveError('El material es obligatorio')
      return
    }
    if (!form.diametroMm || Number(form.diametroMm) <= 0) {
      setSaveError('El diámetro debe ser mayor a cero')
      return
    }
    if (!form.longitudM || Number(form.longitudM) <= 0) {
      setSaveError('La longitud debe ser mayor a cero')
      return
    }
    setSaving(true)
    setSaveError(null)
    const payload = {
      sectorId: Number(form.sectorId),
      material: form.material.trim(),
      diametroMm: Number(form.diametroMm),
      longitudM: Number(form.longitudM),
      tramo: form.tramo.trim() || null,
      fechaInstalacion: form.fechaInstalacion || null,
    }
    try {
      if (editing) {
        await api.put(`/tuberias/${editing.id}`, payload)
      } else {
        await api.post('/tuberias', payload)
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
      await api.del(`/tuberias/${deleteTarget.id}`)
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
      key: 'diametroMm',
      header: 'Diámetro',
      align: 'right',
      render: (r) => <span className="num">{formatNumber(r.diametroMm)} mm</span>,
    },
    { key: 'material', header: 'Material' },
    {
      key: 'longitudM',
      header: 'Longitud',
      align: 'right',
      render: (r) => <span className="num">{formatNumber(r.longitudM)} m</span>,
    },
    { key: 'tramo', header: 'Tramo', render: (r) => r.tramo || '—' },
    { key: 'fechaInstalacion', header: 'Instalación', render: (r) => formatDate(r.fechaInstalacion) },
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
            aria-label={`Editar tubería ${r.id}`}
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
            aria-label={`Eliminar tubería ${r.id}`}
          >
            <Trash size={16} weight="bold" />
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader title="Tuberías" subtitle={`${tuberias.length} tuberías registradas`}>
        <Button onClick={openCreate}>
          <Plus size={16} weight="bold" />
          Nueva tubería
        </Button>
      </PageHeader>

      {loadError ? (
        <div className="card">
          <EmptyState icon={FlowArrow} title="No se pudieron cargar las tuberías" text={loadError}>
            <Button variant="secondary" onClick={load}>
              Reintentar
            </Button>
          </EmptyState>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={tuberias}
          loading={loading}
          emptyTitle="Sin tuberías"
          emptyText="Registra la primera tubería de tu red."
        />
      )}

      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? 'Editar tubería' : 'Nueva tubería'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="tuberia-form" disabled={saving}>
              {saving ? <Spinner size={14} /> : null}
              {saving ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear tubería'}
            </Button>
          </>
        }
      >
        <form id="tuberia-form" onSubmit={handleSave} className="space-y-4">
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
          <Field label="Material" required>
            <input
              className={inputCls}
              value={form.material}
              onChange={(e) => setForm({ ...form, material: e.target.value })}
              placeholder="Ej. PVC"
              required
            />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Diámetro (mm)" required>
              <input
                type="number"
                step="0.01"
                min="0"
                className={inputCls}
                value={form.diametroMm}
                onChange={(e) => setForm({ ...form, diametroMm: e.target.value })}
                placeholder="Ej. 110"
                required
              />
            </Field>
            <Field label="Longitud (m)" required>
              <input
                type="number"
                step="0.01"
                min="0"
                className={inputCls}
                value={form.longitudM}
                onChange={(e) => setForm({ ...form, longitudM: e.target.value })}
                placeholder="Ej. 250.5"
                required
              />
            </Field>
          </div>
          <Field label="Tramo" hint="Opcional. Identifica el tramo de la red.">
            <input
              className={inputCls}
              value={form.tramo}
              onChange={(e) => setForm({ ...form, tramo: e.target.value })}
              placeholder="Ej. Tramo A-3"
            />
          </Field>
          <Field label="Fecha de instalación">
            <input
              type="date"
              className={inputCls}
              value={form.fechaInstalacion}
              onChange={(e) => setForm({ ...form, fechaInstalacion: e.target.value })}
            />
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        busy={deleting}
        title="Eliminar tubería"
        confirmLabel="Eliminar"
        message={`¿Eliminar la tubería ${deleteTarget?.material} del sector "${deleteTarget?.sectorNombre}"? Esta acción no se puede deshacer.`}
      />
    </div>
  )
}