import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CircleNotch, Drop } from '@phosphor-icons/react'
import { api, auth } from '../api/client'
import { Button, Field, inputCls } from '../components/ui'

export default function Login() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await api.post('/auth/login', { username, password })
      auth.setToken(data.token)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.message || 'No se pudo iniciar sesión')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-dvh items-center justify-center bg-slate-100 px-4 dark:bg-slate-950">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-accent-600 text-white dark:bg-accent-500 dark:text-accent-950">
            <Drop size={26} weight="fill" />
          </div>
          <div className="text-center">
            <h1 className="text-xl font-semibold tracking-tight">Servicio de Agua</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400">Inicia sesión para continuar</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="card space-y-4 p-6">
          <Field label="Usuario" required>
            <input
              className={inputCls}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="admin"
              autoComplete="username"
            />
          </Field>
          <Field label="Contraseña" required>
            <input
              className={inputCls}
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
            />
          </Field>

          {error && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600 dark:bg-red-500/10 dark:text-red-400">
              {error}
            </p>
          )}

          <Button type="submit" block disabled={loading}>
            {loading && <CircleNotch size={18} className="animate-spin" />}
            {loading ? 'Ingresando…' : 'Ingresar'}
          </Button>
        </form>
      </div>
    </div>
  )
}