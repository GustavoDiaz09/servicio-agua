import { Component } from 'react'
import { ArrowCounterClockwise, WarningCircle } from '@phosphor-icons/react'
import { Button } from './ui'

export default class ErrorBoundary extends Component {
  state = { hasError: false, error: null }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, info) {
    console.error('ErrorBoundary:', error, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="card mx-auto mt-10 max-w-md p-6 text-center">
          <WarningCircle size={32} weight="duotone" className="mx-auto text-red-500" />
          <h2 className="mt-3 text-lg font-semibold text-slate-900 dark:text-slate-100">
            Algo salió mal en esta sección
          </h2>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Ocurrió un error inesperado al cargar la página. Podés reintentarlo o actualizar la vista.
          </p>
          {this.state.error && (
            <p className="mt-3 truncate rounded-lg bg-red-50 px-3 py-2 font-mono text-xs text-red-700 dark:bg-red-500/10 dark:text-red-300">
              {String(this.state.error)}
            </p>
          )}
          <div className="mt-4 flex justify-center gap-2">
            <Button onClick={() => this.setState({ hasError: false, error: null })}>
              <ArrowCounterClockwise size={16} weight="bold" />
              Volver a intentar
            </Button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}