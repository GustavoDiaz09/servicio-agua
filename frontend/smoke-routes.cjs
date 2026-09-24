const { spawn } = require('child_process')
const { existsSync } = require('fs')

const FRONT = process.env.SMOKE_URL || 'http://localhost:5173'
const BACK = process.env.SMOKE_BACKEND || 'http://localhost:8080'
const USER = process.env.SMOKE_USER || 'admin'
const PASS = process.env.SMOKE_PASS || 'admin123'
const PORT = Number(process.env.SMOKE_CDP_PORT || 9222)
const PROFILE = process.env.SMOKE_PROFILE || 'C:\\Users\\gusta\\AppData\\Local\\Temp\\opencode\\smoke-profile'
const WAIT_MS = Number(process.env.SMOKE_WAIT || 3500)

const ROUTES = [
  '/',
  '/clientes',
  '/tomas',
  '/medidores',
  '/lecturas',
  '/tarifas',
  '/facturas',
  '/pagos',
  '/sectores',
  '/tuberias',
  '/fugas',
  '/mantenimientos',
  '/usuarios',
  '/reportes',
  '/login',
]

const EDGE = process.env.EDGE_PATH || [
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
].find((p) => existsSync(p))

const delay = (ms) => new Promise((r) => setTimeout(r, ms))

if (!EDGE) {
  console.error('SMOKE: no se encontró Edge/Chrome. Definir EDGE_PATH.')
  process.exit(1)
}

async function main() {
  const chrome = spawn(EDGE, [
    '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
    `--remote-debugging-port=${PORT}`, `--user-data-dir=${PROFILE}`, 'about:blank',
  ])

  const loginRes = await fetch(`${BACK}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USER, password: PASS }),
  })
  const loginJson = await loginRes.json()
  if (!loginRes.ok || !loginJson.data || !loginJson.data.token) {
    console.error(`SMOKE: login falló (${loginRes.status}). ¿Está el backend en ${BACK}?`)
    chrome.kill()
    process.exit(1)
  }
  const token = loginJson.data.token
  console.log(`SMOKE: login OK (${loginJson.data.username}/${loginJson.data.rol}) · ${FRONT} → ${BACK}`)

  let page
  for (let i = 0; i < 40; i++) {
    try {
      const list = await (await fetch(`http://localhost:${PORT}/json/list`)).json()
      page = list.find((t) => t.type === 'page')
      if (page) break
    } catch {}
    await delay(500)
  }
  if (!page) {
    console.error('SMOKE: no se pudo conectar al navegador headless.')
    chrome.kill()
    process.exit(1)
  }

  const ws = new WebSocket(page.webSocketDebuggerUrl)
  await new Promise((r) => ws.addEventListener('open', r))
  let id = 0
  const pend = new Map()
  let exceptions = []

  const send = (method, params = {}) => {
    const mid = ++id
    ws.send(JSON.stringify({ id: mid, method, params }))
    return new Promise((res) => pend.set(mid, res))
  }
  ws.addEventListener('message', (ev) => {
    const msg = JSON.parse(ev.data)
    if (msg.id && pend.has(msg.id)) { pend.get(msg.id)(msg); pend.delete(msg.id) }
    if (msg.method === 'Runtime.exceptionThrown') {
      const d = msg.params.exceptionDetails
      const first = d.stackTrace && d.stackTrace.callFrames[0]
      exceptions.push({
        text: (d.exception && d.exception.description || d.text).split('\n')[0],
        at: first ? `${first.functionName} (${first.url}:${first.lineNumber})` : '',
      })
    }
  })

  await send('Runtime.enable')
  await send('Page.enable')

  await send('Page.navigate', { url: `${FRONT}/login` })
  await delay(2000)
  await send('Runtime.evaluate', { expression: `localStorage.setItem('agua_token', '${token}'); 'ok'` })
  console.log('SMOKE: token inyectado en localStorage.')

  const clean = (s) => (s || '').replace(/\s+/g, ' ').trim()

  let failures = 0
  for (const route of ROUTES) {
    exceptions = []
    await send('Page.navigate', { url: `${FRONT}${route}` })
    await delay(WAIT_MS)
    const r = await send('Runtime.evaluate', {
      expression: `JSON.stringify({ text: document.body.innerText, hasAside: !!document.querySelector('aside'), bodyLen: document.body.innerHTML.length })`,
      returnByValue: true,
    })
    const state = JSON.parse(r.result.result.value)
    const trouble = exceptions.filter((e) => e.text.includes('ReferenceError') || e.text.includes('TypeError') || e.text.includes('is not defined'))
    const isBoundary = clean(state.text).includes('Algo salió mal en esta sección')
    const isBlank = state.bodyLen < 200 || (state.hasAside && clean(state.text).length < 10)

    const bad = trouble.length > 0 || isBoundary || isBlank
    if (bad) failures++

    const detail = trouble[0] ? `${trouble[0].text} @ ${trouble[0].at}` : isBoundary ? 'error boundary renderizado' : isBlank ? 'página en blanco' : ''
    console.log(`${bad ? 'FAIL' : ' OK '}  ${route}${detail ? '  →  ' + detail : ''}`)
  }

  ws.close()
  chrome.kill()
  console.log(failures ? `\nSMOKE: ${failures} ruta(s) con problemas.` : '\nSMOKE: todas las rutas renderizan correctamente.')
  process.exit(failures ? 1 : 0)
}

main().catch((e) => { console.error('SMOKE:', e); process.exit(1) })