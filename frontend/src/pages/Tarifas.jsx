import { useCallback, useEffect, useState } from 'react'
import { CurrencyCircleDollar, Drop, Plus, Trash, WarningCircle } from '@phosphor-icons/react'
import { api } from '../api/client'
import { formatDate, formatMoney, formatNumber, m3 } from '../lib/format'
import {
  Badge,
  Button,
  CheckCircle,
  ConfirmDialog,
  DataTable,
  Field,
  Modal,
  PageHeader,
  Spinner,
  StatCard,
  inputCls,
} from '../components/ui'

const vacio = {
  nombre: '',
  desdeM3: '',
  hastaM3: '',
  precioPorM3: '',
  cargoFijo: '',
  vigenteDesde: '',
  vigenteHasta: '',
}

function vigenteHoy(tarifa) {
  const hoy = new Date().toISOString().slice(0, 10)
  if (tarifa.vigenteDesde > hoy) return false
  if (tarifa.vigenteHasta && tarifa.vigenteHasta < hoy) return false
  return true
}

export default function Tarifas() {
  const [tarifas, setTarifas] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [filtroVigencia, setFiltroVigencia] = useState('todas')

  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState(vacio)
  const [formErrors, setFormErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const [confirmTarget, setConfirmTarget] = useState(null)
  const [eliminando, setEliminando] = useState(false)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const ruta = filtroVigencia === 'vigentes' ? '/tarifas/vigentes' : '/tarifas'
      setTarifas(await api.get(ruta))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [filtroVigencia])

  useEffect(() => {
    cargar()
  }, [cargar])

  useEffect(() => {
    if (!success) return
    const t = setTimeout(() => setSuccess(''), 4000)
    return () => clearTimeout(t)
  }, [success])

  const abrirModal = () => {
    const hoy = new Date().toISOString().slice(0, 10)
    setForm({ ...vacio, vigenteDesde: hoy })
    setFormErrors({})
    setModalOpen(true)
  }

  const validar = () => {
    const errs = {}
    if (!form.nombre.trim()) errs.nombre = 'El nombre es obligatorio'
    const desde = Number(form.desdeM3)
    const hasta = Number(form.hastaM3)
    if (form.desdeM3 === '' || Number.isNaN(desde) || desde < 0) {
      errs.desdeM3 = 'Ingresá un tramo inicial válido'
    }
    if (form.hastaM3 === '' || Number.isNaN(hasta) || hasta < 1) {
      errs.hastaM3 = 'Ingresá un tramo final válido'
    } else if (!Number.isNaN(desde) && hasta <= desde) {
      errs.hastaM3 = 'Debe ser mayor que el tramo inicial'
    }
    const precio = Number(form.precioPorM3)
    if (form.precioPorM3 === '' || Number.isNaN(precio) || precio < 0) {
      errs.precioPorM3 = 'Ingresá un precio válido'
    }
    if (form.cargoFijo !== '' && (Number.isNaN(Number(form.cargoFijo)) || Number(form.cargoFijo) < 0)) {
      errs.cargoFijo = 'El cargo fijo no puede ser negativo'
    }
    if (!form.vigenteDesde) errs.vigenteDesde = 'La fecha de inicio es obligatoria'
    if (form.vigenteHasta && form.vigenteHasta < form.vigenteDesde) {
      errs.vigenteHasta = 'No puede ser anterior al inicio de vigencia'
    }
    return errs
  }

  const guardar = async (e) => {
    e.preventDefault()
    const errs = validar()
    setFormErrors(errs)
    if (Object.keys(errs).length) return
    setSaving(true)
    try {
      await api.post('/tarifas', {
        nombre: form.nombre.trim(),
        desdeM3: Number(form.desdeM3),
        hastaM3: Number(form.hastaM3),
        precioPorM3: Number(form.precioPorM3),
        cargoFijo: form.cargoFijo === '' ? null : Number(form.cargoFijo),
        vigenteDesde: form.vigenteDesde,
        vigenteHasta: form.vigenteHasta || null,
      })
      setModalOpen(false)
      setSuccess('Tarifa creada correctamente')
      await cargar()
    } catch (err) {
      setFormErrors({ global: err.message })
    } finally {
      setSaving(false)
    }
  }

  const confirmarEliminacion = async () => {
    setEliminando(true)
    try {
      await api.del(`/tarifas/${confirmTarget.id}`)
      setConfirmTarget(null)
      setSuccess('Tarifa eliminada correctamente')
      await cargar()
    } catch (err) {
      setConfirmTarget(null)
      setError(err.message)
    } finally {
      setEliminando(false)
    }
  }

  const total = tarifas.length
  const vigentesHoyCount = tarifas.filter(vigenteHoy).length
  const precioMaximo = tarifas.reduce((acc, t) => Math.max(acc, Number(t.precioPorM3) || 0), 0)

  const columns = [
    {
      key: 'nombre',
      header: 'Nombre',
      render: (r) => <span className="font-medium text-slate-900 dark:text-slate-100">{r.nombre}</span>,
    },
    {
      key: 'rango',
      header: 'Rango',
      render: (r) => (
        <span className="num">
          {m3(r.desdeM3)} – {r.hastaM3 === null || r.hastaM3 === undefined ? '∞' : m3(r.hastaM3)}
        </span>
      ),
    },
    {
      key: 'precioPorM3',
      header: 'Precio por m³',
      align: 'right',
      render: (r) => <span className="num font-medium text-accent-700 dark:text-accent-300">{formatMoney(r.precioPorM3)}</span>,
    },
    {
      key: 'cargoFijo',
      header: 'Cargo fijo',
      align: 'right',
      render: (r) => <span className="num">{formatMoney(r.cargoFijo)}</span>,
    },
    {
      key: 'vigencia',
      header: 'Vigencia',
      render: (r) => (
        <span className="whitespace-nowrap">
          {formatDate(r.vigenteDesde)} → {formatDate(r.vigenteHasta)}
        </span>
      ),
    },
    {
      key: 'vigenteHoy',
      header: 'Estado hoy',
      render: (r) =>
        vigenteHoy(r) ? <Badge tone="green">Vigente hoy</Badge> : <Badge tone="red">No vigente</Badge>,
    },
    {
      key: 'acciones',
      header: '',
      align: 'right',
      render: (r) => (
        <button
          onClick={() => setConfirmTarget(r)}
          className="inline-flex items-center gap-1.5 text-sm font-medium text-red-600 transition-colors hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
        >
          <Trash size={15} weight="bold" />
          Eliminar
        </button>
      ),
    },
  ]

  const segmCls = {
    activo:
      'rounded-md bg-white px-3 py-1.5 text-sm font-medium text-slate-900 shadow-sm dark:bg-slate-900 dark:text-slate-100',
    inactivo:
      'rounded-md px-3 py-1.5 text-sm font-medium text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200',
  }

  return (
    <div>
      <PageHeader title="Tarifas" subtitle="Tramos de consumo, precios por m³ y vigencia">
        <div className="inline-flex rounded-lg bg-slate-100 p-0.5 dark:bg-slate-800">
          <button
            className={filtroVigencia === 'todas' ? segmCls.activo : segmCls.inactivo}
            onClick={() => setFiltroVigencia('todas')}
          >
            Todas
          </button>
          <button
            className={filtroVigencia === 'vigentes' ? segmCls.activo : segmCls.inactivo}
            onClick={() => setFiltroVigencia('vigentes')}
          >
            Vigentes hoy
          </button>
        </div>
        <Button onClick={abrirModal}>
          <Plus size={18} weight="bold" />
          Nueva tarifa
        </Button>
      </PageHeader>

      {success && (
        <div className="card mb-4 flex items-center gap-2 border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300">
          <CheckCircle size={18} weight="duotone" />
          {success}
        </div>
      )}
      {error && (
        <div className="card mb-4 flex items-center gap-2 border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">
          <WarningCircle size={18} weight="duotone" />
          {error}
        </div>
      )}

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard label="Tarifas registradas" value={formatNumber(total)} icon={Drop} tone="sky" />
        <StatCard
          label="Vigentes hoy"
          value={formatNumber(vigentesHoyCount)}
          icon={CheckCircle}
          tone="green"
        />
        <StatCard
          label="Precio máximo por m³"
          value={formatMoney(precioMaximo)}
          icon={CurrencyCircleDollar}
          tone="amber"
        />
      </div>

      <DataTable
        columns={columns}
        data={tarifas}
        loading={loading}
        emptyTitle="Sin tarifas"
        emptyText={
          filtroVigencia === 'vigentes'
            ? 'No hay tarifas vigentes en la fecha actual.'
            : 'No hay tarifas registradas todavía.'
        }
      />

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Nueva tarifa"
        size="md"
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button onClick={guardar} disabled={saving}>
              {saving && <Spinner size={14} />}
              {saving ? 'Guardando…' : 'Guardar tarifa'}
            </Button>
          </>
        }
      >
        <form onSubmit={guardar} className="space-y-4">
          {formErrors.global && (
            <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">
              {formErrors.global}
            </div>
          )}
          <Field label="Nombre" required error={formErrors.nombre}>
            <input
              type="text"
              maxLength={100}
              className={inputCls}
              placeholder="Ej: Tramo 1 - residencial"
              value={form.nombre}
              onChange={(e) => setForm({ ...form, nombre: e.target.value })}
            />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Desde (m³)" required error={formErrors.desdeM3}>
              <input
                type="number"
                min="0"
                step="1"
                className={inputCls}
                placeholder="Ej: 0"
                value={form.desdeM3}
                onChange={(e) => setForm({ ...form, desdeM3: e.target.value })}
              />
            </Field>
            <Field label="Hasta (m³)" required error={formErrors.hastaM3}>
              <input
                type="number"
                min="1"
                step="1"
                className={inputCls}
                placeholder="Ej: 20"
                value={form.hastaM3}
                onChange={(e) => setForm({ ...form, hastaM3: e.target.value })}
              />
            </Field>
            <Field label="Precio por m³" required error={formErrors.precioPorM3}>
              <input
                type="number"
                min="0"
                step="0.01"
                className={inputCls}
                placeholder="Ej: 3200.00"
                value={form.precioPorM3}
                onChange={(e) => setForm({ ...form, precioPorM3: e.target.value })}
              />
            </Field>
            <Field label="Cargo fijo" error={formErrors.cargoFijo} hint="Opcional; si se omite se usa 0">
              <input
                type="number"
                min="0"
                step="0.01"
                className={inputCls}
                placeholder="Ej: 4500.00"
                value={form.cargoFijo}
                onChange={(e) => setForm({ ...form, cargoFijo: e.target.value })}
              />
            </Field>
            <Field label="Inicio de vigencia" required error={formErrors.vigenteDesde}>
              <input
                type="date"
                className={inputCls}
                value={form.vigenteDesde}
                onChange={(e) => setForm({ ...form, vigenteDesde: e.target.value })}
              />
            </Field>
            <Field label="Fin de vigencia" error={formErrors.vigenteHasta} hint="Opcional; sin límite si se omite">
              <input
                type="date"
                className={inputCls}
                value={form.vigenteHasta}
                onChange={(e) => setForm({ ...form, vigenteHasta: e.target.value })}
              />
            </Field>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!confirmTarget}
        onClose={() => setConfirmTarget(null)}
        onConfirm={confirmarEliminacion}
        busy={eliminando}
        title="Eliminar tarifa"
        message={`¿Eliminar la tarifa "${confirmTarget?.nombre}"? Esta acción no se puede deshacer.`}
        confirmLabel="Eliminar tarifa"
      />
    </div>
  )
}