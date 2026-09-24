import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import { Skeleton } from './components/ui'
import { auth } from './api/client'

const Login = lazy(() => import('./pages/Login'))
const Dashboard = lazy(() => import('./pages/Dashboard'))
const Clientes = lazy(() => import('./pages/Clientes'))
const Tomas = lazy(() => import('./pages/Tomas'))
const Medidores = lazy(() => import('./pages/Medidores'))
const Lecturas = lazy(() => import('./pages/Lecturas'))
const Tarifas = lazy(() => import('./pages/Tarifas'))
const Facturas = lazy(() => import('./pages/Facturas'))
const Pagos = lazy(() => import('./pages/Pagos'))
const Sectores = lazy(() => import('./pages/Sectores'))
const Tuberias = lazy(() => import('./pages/Tuberias'))
const Fugas = lazy(() => import('./pages/Fugas'))
const Mantenimientos = lazy(() => import('./pages/Mantenimientos'))
const Reportes = lazy(() => import('./pages/Reportes'))

function Fallback() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="card p-5">
            <Skeleton className="mb-3 h-3 w-28" />
            <Skeleton className="h-7 w-20" />
          </div>
        ))}
      </div>
      <div className="card p-6">
        <Skeleton className="mb-4 h-4 w-40" />
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      </div>
    </div>
  )
}

const page = (Component) => (
  <Suspense fallback={<Fallback />}>
    <Component />
  </Suspense>
)

function RequireAuth({ children }) {
  return auth.getToken() ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={page(Login)} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={page(Dashboard)} />
        <Route path="clientes" element={page(Clientes)} />
        <Route path="tomas" element={page(Tomas)} />
        <Route path="medidores" element={page(Medidores)} />
        <Route path="lecturas" element={page(Lecturas)} />
        <Route path="tarifas" element={page(Tarifas)} />
        <Route path="facturas" element={page(Facturas)} />
        <Route path="pagos" element={page(Pagos)} />
        <Route path="sectores" element={page(Sectores)} />
        <Route path="tuberias" element={page(Tuberias)} />
        <Route path="fugas" element={page(Fugas)} />
        <Route path="mantenimientos" element={page(Mantenimientos)} />
        <Route path="reportes" element={page(Reportes)} />
        <Route path="*" element={page(Dashboard)} />
      </Route>
    </Routes>
  )
}