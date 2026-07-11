let socket
let reconnectTimer
let manualClose = false

const wsUrl = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/events`
}

export const connectRealtime = () => {
  if (socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(socket.readyState)) return socket
  manualClose = false
  socket = new WebSocket(wsUrl())
  socket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data)
      window.dispatchEvent(new CustomEvent('iot-realtime', { detail: message }))
    } catch (error) {
      console.warn('Realtime message ignored', error)
    }
  }
  socket.onclose = () => {
    if (manualClose) return
    window.clearTimeout(reconnectTimer)
    reconnectTimer = window.setTimeout(connectRealtime, 3000)
  }
  socket.onerror = () => socket.close()
  return socket
}

export const closeRealtime = () => {
  manualClose = true
  window.clearTimeout(reconnectTimer)
  if (socket) socket.close()
  socket = null
}
