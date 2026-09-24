import { useCallback, useEffect, useState } from 'react'
import { CheckCircle, Key, Plus, Prohibit, ShieldCheck, UserCirclePlus } from '@phosphor-icons/react'
import { api, auth } from '../api/client'
import { Badge, Button, DataTable, Field, Modal, PageHeader, Spinner, inputCls } from '../components/ui'
import { formatDateTime } from '../lib/format'

const formVacio = { username: '', password: '' }

function ErrorBanner({ children }) {
  if (!children) return null
  return (
    <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300">
      {children}
    </div>
  )
}

export default function Usuarios() {
  const [usuarioActual, setUsuarioActual] = useState(null)
  const [usuarios, setUsuarios] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [crearOpen, setCrearOpen] = useState(false)
  const [form, setForm] = useState(formVacio)
  const [formError, setFormError] = useState('')
  const [creando, setCreando] = useState(false)

  const [estadoTarget, setEstadoTarget] = useState(null)
  const [estadoError, setEstadoError] = useState('')
  const [cambiando, setCambiando] = useState(false)

  const [resetOpen, setResetOpen] = useState(false)
  const [resetTarget, setResetTarget] = useState(null)
  const [resetPassword, setResetPassword] = useState('')
  const [resetError, setResetError] = useState('')
  const [reseteando, setReseteando] = useState(false)

  useEffect(() => {
    try {
      const token = auth.getToken()
      if (!token) return
      const claims = JSON.parse(atob(token.split('.')[1]))
      if (claims.sub) setUsuarioActual(claims.sub)
    } catch {
      /* token no decodificable */
    }
  }, [])

  const cargar = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await api.get('/usuarios')
      setUsuarios(res || [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    cargar()
  }, [cargar])

  function abrirCrear() {
    setForm(formVacio)
    setFormError('')
    setCrearOpen(true)
  }

  async function crear(e) {
    e.preventDefault()
    setCreando(true)
    setFormError('')
    try {
      await api.post('/usuarios', { username: form.username, password: form.password })
      setCrearOpen(false)
      await cargar()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setCreando(false)
    }
  }

  function abrirEstado(u) {
    setEstadoTarget(u)
    setEstadoError('')
  }

  async function guardarEstado() {
    setCambiando(true)
    setEstadoError('')
    try {
      await api.put(`/usuarios/${estadoTarget.id}/estado`, { enabled: !estadoTarget.enabled })
      setEstadoTarget(null)
      await cargar()
    } catch (err) {
      setEstadoError(err.message)
    } finally {
      setCambiando(false)
    }
  }

  function abrirReset(u) {
    setResetTarget(u)
    setResetPassword('')
    setResetError('')
    setResetOpen(true)
  }

  async function resetClave() {
    setReseteando(true)
    setResetError('')
    try {
      await api.put(`/usuarios/${resetTarget.id}/password`, { newPassword: resetPassword })
      setResetOpen(false)
      await cargar()
    } catch (err) {
      setResetError(err.message)
    } finally {
      setReseteando(false)
    }
  }

  const acciones = (u) => (
    <div className="flex items-center gap-1">
      <Button
        variant="ghost"
        onClick={() => abrirEstado(u)}
        disabled={u.username === usuarioActual}
        title={
          u.username === usuarioActual
            ? 'No puede deshabilitarse a sí mismo'
            : u.enabled
              ? 'Deshabilitar usuario'
              : 'Habilitar usuario'
        }
      >
        {u.enabled ? <Prohibit size={18} weight="duotone" /> : <CheckCircle size={18} weight="duotone" />}
      </Button>
      <Button variant="ghost" onClick={() => abrirReset(u)} title="Restablecer contraseña">
        <Key size={18} weight="duotone" />
      </Button>
    </div>
  )

  const columns = [
    { key: 'username', header: 'Usuario', className: 'font-medium' },
    {
      key: 'rol',
      header: 'Rol',
      render: (c) => <Badge tone="violet">{c.rol || 'ADMIN'}</Badge>,
    },
    {
      key: 'fechaCreacion',
      header: 'Creado',
      render: (c) => <span className="text-slate-500 dark:text-slate-400">{formatDateTime(c.fechaCreacion)}</span>,
    },
    {
      key: 'estado',
      header: 'Estado',
      render: (c) =>
        c.enabled ? <Badge tone="green">Activo</Badge> : <Badge tone="red">Deshabilitado</Badge>,
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (c) => acciones(c),
    },
  ]

  return (
    <div>
      <PageHeader title="Usuarios" subtitle="Administra los accesos al sistema de servicio de agua potable">
        <Button onClick={abrirCrear}>
          <Plus size={16} weight="bold" />
          Nuevo usuario
        </Button>
      </PageHeader>

      <ErrorBanner>{error}</ErrorBanner>

      <DataTable
        columns={columns}
        data={usuarios}
        keyField="id"
        loading={loading}
        emptyTitle="Sin usuarios"
        emptyText="Aún no hay usuarios registrados. Crea el primero con el botón «Nuevo usuario»."
      />

      <Modal
        open={crearOpen}
        onClose={creando ? undefined : () => setCrearOpen(false)}
        title="Nuevo usuario"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setCrearOpen(false)} disabled={creando}>
              Cancelar
            </Button>
            <Button type="submit" form="form-usuario" disabled={creando}>
              {creando ? <Spinner size={14} /> : null}
              {creando ? 'Creando…' : 'Crear usuario'}
            </Button>
          </>
        }
      >
        <form id="form-usuario" onSubmit={crear} className="space-y-4">
          <div className="flex items-center gap-3">
            <span className="rounded-full bg-accent-50 p-2 text-accent-600 dark:bg-accent-500/10 dark:text-accent-300">
              <UserCirclePlus size={20} weight="duotone" />
            </span>
            <div>
              <p className="text-sm font-medium">Rol ADMIN</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">Acceso completo al sistema</p>
            </div>
          </div>
          <Field label="Usuario" required>
            <input
              className={inputCls}
              value={form.username}
              onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
              placeholder="nombre_usuario"
              maxLength={50}
              autoComplete="off"
              required
            />
          </Field>
          <Field label="Contraseña" required hint="Mínimo 8 caracteres.">
            <input
              className={inputCls}
              type="password"
              value={form.password}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
              placeholder="••••••••"
              minLength={8}
              maxLength={100}
              autoComplete="new-password"
              required
            />
          </Field>
          {formError && <p className="text-sm text-red-600 dark:text-red-400">{formError}</p>}
        </form>
      </Modal>

      <Modal
        open={estadoTarget !== null}
        onClose={cambiando ? undefined : () => setEstadoTarget(null)}
        title={estadoTarget?.enabled ? 'Deshabilitar usuario' : 'Habilitar usuario'}
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setEstadoTarget(null)} disabled={cambiando}>
              Cancelar
            </Button>
            <Button
              variant={estadoTarget?.enabled ? 'danger' : 'primary'}
              onClick={guardarEstado}
              disabled={cambiando}
            >
              {cambiando ? <Spinner size={14} /> : null}
              {cambiando ? 'Guardando…' : estadoTarget?.enabled ? 'Deshabilitar' : 'Habilitar'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <span
              className={`rounded-full p-2 ${
                estadoTarget?.enabled
                  ? 'bg-red-100 text-red-600 dark:bg-red-500/15 dark:text-red-400'
                  : 'bg-emerald-100 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400'
              }`}
            >
              <ShieldCheck size={20} weight="duotone" />
            </span>
            <div>
              <p className="text-sm font-medium">{estadoTarget?.username}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {estadoTarget?.enabled
                  ? 'El usuario perderá el acceso al sistema.'
                  : 'El usuario podrá volver a iniciar sesión.'}
              </p>
            </div>
          </div>
          {estadoError && <p className="text-sm text-red-600 dark:text-red-400">{estadoError}</p>}
        </div>
      </Modal>

      <Modal
        open={resetOpen}
        onClose={reseteando ? undefined : () => setResetOpen(false)}
        title={`Restablecer contraseña de ${resetTarget?.username || ''}`}
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setResetOpen(false)} disabled={reseteando}>
              Cancelar
            </Button>
            <Button type="submit" form="form-reset" disabled={reseteando || resetPassword.length < 8}>
              {reseteando ? <Spinner size={14} /> : null}
              {reseteando ? 'Guardando…' : 'Restablecer'}
            </Button>
          </>
        }
      >
        <form id="form-reset" onSubmit={resetClave} className="space-y-4">
          <Field label="Nueva contraseña" required hint="Mínimo 8 caracteres.">
            <input
              className={inputCls}
              type="password"
              value={resetPassword}
              onChange={(e) => setResetPassword(e.target.value)}
              placeholder="••••••••"
              minLength={8}
              maxLength={100}
              autoComplete="new-password"
              required
            />
          </Field>
          {resetError && <p className="text-sm text-red-600 dark:text-red-400">{resetError}</p>}
        </form>
      </Modal>
    </div>
  )
}