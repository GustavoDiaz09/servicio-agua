import { useEffect, useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import {
  Calculator,
  ChartLineUp,
  Coins,
  Drop,
  FlowArrow,
  Gauge,
  List,
  MapTrifold,
  Moon,
  PencilSimpleLine,
  Pipe,
  Receipt,
  SquaresFour,
  Sun,
  UsersThree,
  Warning,
  Wrench,
} from '@phosphor-icons/react'
import { useTheme } from '../theme/ThemeContext'

const NAV = [
  {
    section: 'General',
    items: [{ to: '/', label: 'Dashboard', icon: SquaresFour, end: true }],
  },
  {
    section: 'Gestión',
    items: [
      { to: '/clientes', label: 'Clientes', icon: UsersThree },
      { to: '/tomas', label: 'Tomas', icon: Pipe },
      { to: '/medidores', label: 'Medidores', icon: Gauge },
      { to: '/lecturas', label: 'Lecturas', icon: PencilSimpleLine },
      { to: '/tarifas', label: 'Tarifas', icon: Calculator },
      { to: '/facturas', label: 'Facturas', icon: Receipt },
      { to: '/pagos', label: 'Pagos', icon: Coins },
    ],
  },
  {
    section: 'Red',
    items: [
      { to: '/sectores', label: 'Sectores', icon: MapTrifold },
      { to: '/tuberias', label: 'Tuberías', icon: FlowArrow },
      { to: '/fugas', label: 'Fugas', icon: Warning },
      { to: '/mantenimientos', label: 'Mantenimientos', icon: Wrench },
    ],
  },
  {
    section: 'Análisis',
    items: [{ to: '/reportes', label: 'Reportes', icon: ChartLineUp }],
  },
]

function Brand() {
  return (
    <div className="flex items-center gap-3 px-6 py-5">
      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent-600 text-white dark:bg-accent-500 dark:text-accent-950">
        <Drop size={22} weight="fill" />
      </div>
      <div className="leading-tight">
        <p className="text-sm font-semibold tracking-tight">Servicio de Agua</p>
        <p className="text-xs text-slate-500 dark:text-slate-400">Gestión potable</p>
      </div>
    </div>
  )
}

function SidebarContent({ onNavigate }) {
  return (
    <>
      <Brand />
      <nav className="flex-1 space-y-6 overflow-y-auto px-3 pb-6">
        {NAV.map((group) => (
          <div key={group.section}>
            <p className="px-3 pb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
              {group.section}
            </p>
            <ul className="space-y-0.5">
              {group.items.map((item) => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.end}
                    onClick={onNavigate}
                    className={({ isActive }) =>
                      `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                        isActive
                          ? 'bg-accent-50 text-accent-700 dark:bg-accent-500/10 dark:text-accent-300'
                          : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-slate-100'
                      }`
                    }
                  >
                    <item.icon size={18} weight="duotone" />
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </nav>
      <p className="border-t border-slate-200 px-6 py-4 text-xs text-slate-400 dark:border-slate-800 dark:text-slate-500">
        v1.0 · Fase D
      </p>
    </>
  )
}

function ApiStatus() {
  const [status, setStatus] = useState('loading')
  useEffect(() => {
    fetch('/api/clientes')
      .then((res) => setStatus(res.ok ? 'ok' : 'error'))
      .catch(() => setStatus('error'))
  }, [])

  const map = {
    loading: { color: 'bg-slate-400', label: 'Verificando API…' },
    ok: { color: 'bg-emerald-500', label: 'API conectada' },
    error: { color: 'bg-red-500', label: 'API sin conexión' },
  }
  const s = map[status]
  return (
    <span className="hidden items-center gap-2 rounded-full border border-slate-200 px-3 py-1 text-xs text-slate-500 dark:border-slate-700 dark:text-slate-400 sm:inline-flex">
      <span className={`h-2 w-2 rounded-full ${s.color}`} />
      {s.label}
    </span>
  )
}

export default function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const { theme: mode, toggle } = useTheme()

  return (
    <div className="min-h-dvh">
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 flex-col border-r border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900 lg:flex">
        <SidebarContent />
      </aside>

      {menuOpen && (
        <>
          <div
            className="fixed inset-0 z-[45] bg-slate-950/40 backdrop-blur-sm lg:hidden"
            onClick={() => setMenuOpen(false)}
          />
          <aside className="fixed inset-y-0 left-0 z-[46] flex w-64 flex-col bg-white dark:bg-slate-900 lg:hidden">
            <SidebarContent onNavigate={() => setMenuOpen(false)} />
          </aside>
        </>
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200 bg-slate-100/85 px-4 backdrop-blur dark:border-slate-800 dark:bg-slate-950/85 lg:px-8">
          <button
            className="rounded-lg p-2 text-slate-500 hover:bg-slate-200/70 dark:hover:bg-slate-800 lg:hidden"
            onClick={() => setMenuOpen(true)}
            aria-label="Abrir menú"
          >
            <List size={22} />
          </button>
          <div className="hidden lg:block">
            <ApiStatus />
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={toggle}
              className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
              aria-label="Cambiar tema"
            >
              {mode === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
              <span className="hidden sm:inline">{mode === 'dark' ? 'Claro' : 'Oscuro'}</span>
            </button>
          </div>
        </header>

        <main className="mx-auto w-full max-w-[1400px] px-4 py-8 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}