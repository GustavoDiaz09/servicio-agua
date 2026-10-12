import { useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowCounterClockwise, Drop, Plus, WarningCircle } from '@phosphor-icons/react'
import { api } from '../api/client'
import { formatDate, formatNumber, m3 } from '../lib/format'
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
  estadoTone,
  inputCls,
} from '../components/ui'

const estadoLecturaTexto = {
  ACTIVA: 'Activa',
  ESTIMADA: 'Estimada',
  ANULADA: 'Anulada',
}

const vacio = { medidorId: '', valorLectura: '', fechaLectura: '', observacion: '' }

export default function Lecturas() {
  const [medidores, setMedidores] = useState([])
  const [lecturas, setLecturas] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [filtroMedidor, setFiltroMedidor] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState(vacio)
  const [formErrors, setFormErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const [confirmTarget, setConfirmTarget] = useState(null)
  const [anulando, setAnulando] = useState(false)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const ruta = filtroMedidor ? `/lecturas/medidor/${filtroMedidor}` : '/lecturas'
      setLecturas(await api.get(ruta))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [filtroMedidor])

  useEffect(() => {
    api
      .get('/medidores')
      .then((data) => setMedidores(data || []))
      .catch((e) => setError('No se pudo cargar la lista de medidores: ' + e.message))
  }, [])

  useEffect(() => {
    cargar()
  }, [cargar])

  useEffect(() => {
    if (!success) return
    const t = setTimeout(() => setSuccess(''), 4000)
    return () => clearTimeout(t)
  }, [success])

  const mapMedidor = useMemo(() => {
    const m = new Map(medidores.map((med) => [med.id, med]))
    return m
  }, [medidores])

  const valorAnteriorDe = useCallback(
    (medidorId) => {
      const id = Number(medidorId)
      const activas = lecturas.filter((l) => l.medidorId === id && l.estado === 'ACTIVA')
      if (activas.length) {
        return activas.reduce((a, b) => (a.fechaLectura >= b.fechaLectura ? a : b)).valorLectura
      }
      const med = mapMedidor.get(id)
      return med?.lecturaAnteriorAjuste ?? 0
    },
    [lecturas, mapMedidor],
  )

  const anterior = form.medidorId ? valorAnteriorDe(form.medidorId) : null
  const consumoEstimado =
    anterior !== null && form.valorLectura !== '' ? Number(form.valorLectura) - anterior : null

  const abrirModal = () => {
    const inicial = String(filtroMedidor || medidores[0]?.id || '')
    setForm({ ...vacio, medidorId: inicial, fechaLectura: new Date().toISOString().slice(0, 10) })
    setFormErrors({})
    setModalOpen(true)
  }

  const validar = () => {
    const errs = {}
    if (!form.medidorId) errs.medidorId = 'Seleccioná un medidor'
    const valor = Number(form.valorLectura)
    if (form.valorLectura === '' || Number.isNaN(valor) || valor < 0) {
      errs.valorLectura = 'Ingresá un valor de lectura válido'
    } else if (consumoEstimado !== null && consumoEstimado < 0) {
      errs.valorLectura = 'El valor debe ser mayor o igual al registro anterior'
    }
    if (!form.fechaLectura) errs.fechaLectura = 'La fecha de lectura es obligatoria'
    return errs
  }

  const guardar = async (e) => {
    e.preventDefault()
    const errs = validar()
    setFormErrors(errs)
    if (Object.keys(errs).length) return
    setSaving(true)
    try {
      await api.post('/lecturas', {
        medidorId: Number(form.medidorId),
        valorLectura: Number(form.valorLectura),
        fechaLectura: form.fechaLectura,
        observacion: form.observacion.trim() || null,
      })
      setModalOpen(false)
      setSuccess('Lectura registrada correctamente')
      await cargar()
    } catch (e) {
      setFormErrors({ global: e.message })
    } finally {
      setSaving(false)
    }
  }

  const confirmarAnulacion = async () => {
    setAnulando(true)
    try {
      await api.put(`/lecturas/${confirmTarget.id}/anular`)
      setConfirmTarget(null)
      setSuccess('Lectura anulada correctamente')
      await cargar()
    } catch (e) {
      setConfirmTarget(null)
      setError(e.message)
    } finally {
      setAnulando(false)
    }
  }

  const totalLecturas = lecturas.length
  const consumoTotal = lecturas
    .filter((l) => l.estado !== 'ANULADA')
    .reduce((acc, l) => acc + (l.consumoM3 ?? 0), 0)
  const anuladas = lecturas.filter((l) => l.estado === 'ANULADA').length

  const columns = [
    {
      key: 'numeroSerie',
      header: 'Medidor',
      render: (r) => <span className="font-medium text-slate-900 dark:text-slate-100">{r.numeroSerie}</span>,
    },
    {
      key: 'valorLectura',
      header: 'Valor',
      align: 'right',
      render: (r) => <span className="num">{formatNumber(r.valorLectura)}</span>,
    },
    {
      key: 'consumoM3',
      header: 'Consumo',
      align: 'right',
      render: (r) => (
        <span className="num font-medium text-accent-700 dark:text-accent-300">{m3(r.consumoM3)}</span>
      ),
    },
    { key: 'fechaLectura', header: 'Fecha', render: (r) => formatDate(r.fechaLectura) },
    {
      key: 'estado',
      header: 'Estado',
      render: (r) => (
        <Badge tone={estadoTone(r.estado)}>{estadoLecturaTexto[r.estado] ?? r.estado}</Badge>
      ),
    },
    { key: 'observacion', header: 'Observación', render: (r) => r.observacion || '—' },
    {
      key: 'acciones',
      header: '',
      align: 'right',
      render: (r) =>
        r.estado === 'ACTIVA' ? (
          <button
            onClick={() => setConfirmTarget(r)}
            className="inline-flex items-center gap-1.5 text-sm font-medium text-red-600 transition-colors hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
          >
            <ArrowCounterClockwise size={15} weight="bold" />
            Anular
          </button>
        ) : (
          '—'
        ),
    },
  ]

  const consumoNegativo = consumoEstimado !== null && consumoEstimado < 0

  return (
    <div>
      <PageHeader title="Lecturas" subtitle="Registro e historial de lecturas de los medidores">
        <div className="w-full sm:w-64">
          <Field label="Filtrar por medidor">
            <select
              className={inputCls}
              value={filtroMedidor}
              onChange={(e) => setFiltroMedidor(e.target.value)}
            >
              <option value="">Todos los medidores</option>
              {medidores.map((m) => (
                <option key={m.id} value={m.id}>
                  Serie {m.numeroSerie}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <Button onClick={abrirModal}>
          <Plus size={18} weight="bold" />
          Registrar lectura
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
        <StatCard label="Lecturas registradas" value={formatNumber(totalLecturas)} icon={Drop} tone="sky" />
        <StatCard
          label="Consumo acumulado"
          value={m3(consumoTotal)}
          icon={Drop}
          tone="green"
          hint="Lecturas activas"
        />
        <StatCard label="Lecturas anuladas" value={formatNumber(anuladas)} icon={ArrowCounterClockwise} tone="red" />
      </div>

      <DataTable
        columns={columns}
        data={lecturas}
        loading={loading}
        emptyTitle="Sin lecturas"
        emptyText={
          filtroMedidor
            ? 'Este medidor aún no tiene lecturas registradas.'
            : 'No hay lecturas registradas todavía.'
        }
      />

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Registrar lectura"
        size="md"
        footer={
          <>
            <Button variant="secondary" onClick={() => setModalOpen(false)} disabled={saving}>
              Cancelar
            </Button>
            <Button onClick={guardar} disabled={saving}>
              {saving && <Spinner size={14} />}
              {saving ? 'Guardando…' : 'Guardar lectura'}
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
          <Field label="Medidor" required error={formErrors.medidorId}>
            <select
              className={inputCls}
              value={form.medidorId}
              onChange={(e) => setForm({ ...form, medidorId: e.target.value })}
            >
              <option value="">Seleccioná un medidor</option>
              {medidores.map((m) => (
                <option key={m.id} value={m.id}>
                  Serie {m.numeroSerie}
                </option>
              ))}
            </select>
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Valor anterior" hint={anterior !== null ? m3(anterior) : '—'}>
              <div className="rounded-lg bg-slate-100 px-3 py-2 text-sm text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                {anterior !== null ? m3(anterior) : 'Seleccioná un medidor'}
              </div>
            </Field>
            <Field
              label="Valor actual (lectura)"
              required
              error={formErrors.valorLectura}
            >
              <input
                type="number"
                min="0"
                step="1"
                className={inputCls}
                placeholder="Ej: 012345"
                value={form.valorLectura}
                onChange={(e) => setForm({ ...form, valorLectura: e.target.value })}
              />
              <span
                className={`mt-1 block text-xs ${
                  consumoNegativo
                    ? 'text-red-600 dark:text-red-400'
                    : 'text-slate-500 dark:text-slate-400'
                }`}
              >
                {consumoNegativo
                  ? 'El consumo resultaría negativo; revisá el valor ingresado.'
                  : `Consumo estimado: ${consumoEstimado !== null ? m3(consumoEstimado) : '—'}`}
              </span>
            </Field>
          </div>
          <Field label="Fecha de lectura" required error={formErrors.fechaLectura}>
            <input
              type="date"
              className={inputCls}
              value={form.fechaLectura}
              onChange={(e) => setForm({ ...form, fechaLectura: e.target.value })}
            />
          </Field>
          <Field label="Observación">
            <textarea
              className={`${inputCls} min-h-20 resize-y`}
              maxLength={255}
              placeholder="Notas sobre la toma (opcional)"
              value={form.observacion}
              onChange={(e) => setForm({ ...form, observacion: e.target.value })}
            />
          </Field>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!confirmTarget}
        onClose={() => setConfirmTarget(null)}
        onConfirm={confirmarAnulacion}
        busy={anulando}
        title="Anular lectura"
        message={`¿Anular la lectura de ${formatNumber(confirmTarget?.valorLectura)} del medidor ${
          confirmTarget?.numeroSerie
        } (${formatDate(confirmTarget?.fechaLectura)})? El registro quedará marcado como anulada.`}
        confirmLabel="Anular lectura"
      />
    </div>
  )
}