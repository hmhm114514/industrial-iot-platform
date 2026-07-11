export const CAMERA_ERROR_MESSAGES = Object.freeze({
  NotAllowedError: '摄像头权限被拒绝，请在浏览器设置中允许访问摄像头',
  PermissionDeniedError: '摄像头权限被拒绝，请在浏览器设置中允许访问摄像头',
  NotFoundError: '未找到可用的摄像头设备',
  DevicesNotFoundError: '未找到可用的摄像头设备',
  NotReadableError: '摄像头正被其他应用占用或无法读取',
  TrackStartError: '摄像头正被其他应用占用或无法读取',
  OverconstrainedError: '摄像头不支持所请求的参数',
  ConstraintNotSatisfiedError: '摄像头不支持所请求的参数',
  AbortError: '摄像头连接被中断',
  SecurityError: '当前环境不允许访问摄像头',
  TypeError: '摄像头请求参数无效'
})

export const RECORDING_MIME_TYPES = Object.freeze([
  'video/webm;codecs=vp9,opus',
  'video/webm;codecs=vp8,opus',
  'video/webm;codecs=vp9',
  'video/webm;codecs=vp8',
  'video/webm',
  'video/mp4'
])

export function getCameraErrorMessage(error) {
  if (!error) return '摄像头发生未知错误'
  return CAMERA_ERROR_MESSAGES[error.name] || error.message || '摄像头发生未知错误'
}

export function buildCameraConstraints({ deviceId, width, height, frameRate } = {}) {
  const video = {}
  if (width != null) video.width = { ideal: width }
  if (height != null) video.height = { ideal: height }
  if (frameRate != null) video.frameRate = { ideal: frameRate }
  if (deviceId) video.deviceId = { exact: deviceId }
  return { audio: false, video }
}

export function normalizeCapabilityRange(capability) {
  if (!capability || typeof capability !== 'object') return null
  const min = Number(capability.min)
  const max = Number(capability.max)
  if (!Number.isFinite(min) || !Number.isFinite(max) || min <= 0 || max <= 0 || max < min) return null
  return { min, max }
}

export function buildMaximumVideoConstraints(capabilities = {}) {
  const constraints = {}
  const width = normalizeCapabilityRange(capabilities.width)
  const height = normalizeCapabilityRange(capabilities.height)
  const frameRate = normalizeCapabilityRange(capabilities.frameRate)
  if (width) constraints.width = { ideal: width.max }
  if (height) constraints.height = { ideal: height.max }
  if (frameRate) {
    const target = Math.min(frameRate.max, 30)
    constraints.frameRate = { ideal: target, max: target }
  }
  return constraints
}

export async function maximizeVideoTrack(track) {
  const getSettings = () => {
    try { return typeof track?.getSettings === 'function' ? track.getSettings() || {} : {} } catch { return {} }
  }
  if (!track || typeof track.getCapabilities !== 'function' || typeof track.applyConstraints !== 'function') {
    return { settings: getSettings(), constraints: null, warning: null }
  }
  let constraints
  try {
    constraints = buildMaximumVideoConstraints(track.getCapabilities() || {})
  } catch (error) {
    return {
      settings: getSettings(),
      constraints: null,
      warning: `无法读取摄像头画质能力：${getCameraErrorMessage(error)}`
    }
  }
  if (Object.keys(constraints).length === 0) {
    return { settings: getSettings(), constraints, warning: null }
  }
  try {
    await track.applyConstraints(constraints)
    return { settings: getSettings(), constraints, warning: null }
  } catch (error) {
    return {
      settings: getSettings(),
      constraints,
      warning: `无法应用最佳画质，已保留基础视频流：${getCameraErrorMessage(error)}`
    }
  }
}

export function normalizeVideoDevices(devices = []) {
  return devices
    .filter((device) => device?.kind === 'videoinput')
    .map((device, index) => ({
      deviceId: device.deviceId || '',
      groupId: device.groupId || '',
      kind: 'videoinput',
      label: device.label || `摄像头 ${index + 1}`
    }))
}

export function stopMediaStream(stream) {
  if (!stream || typeof stream.getTracks !== 'function') return
  for (const track of stream.getTracks()) {
    if (track && typeof track.stop === 'function') track.stop()
  }
}

export function selectRecordingMimeType(MediaRecorderCtor, candidates = RECORDING_MIME_TYPES) {
  if (!MediaRecorderCtor || typeof MediaRecorderCtor.isTypeSupported !== 'function') return ''
  return candidates.find((type) => MediaRecorderCtor.isTypeSupported(type)) || ''
}

export function recordingExtension(mimeType = '') {
  const type = mimeType.toLowerCase()
  if (type.includes('mp4')) return 'mp4'
  if (type.includes('ogg')) return 'ogv'
  if (type.includes('matroska')) return 'mkv'
  return 'webm'
}

export function createCameraController(options = {}) {
  const mediaDevices = options.mediaDevices
  const MediaRecorderCtor = options.MediaRecorderCtor
  const urlApi = options.urlApi
  const BlobCtor = options.BlobCtor || globalThis.Blob
  const setTimeoutFn = options.setTimeout || globalThis.setTimeout.bind(globalThis)
  const clearTimeoutFn = options.clearTimeout || globalThis.clearTimeout.bind(globalThis)
  const setIntervalFn = options.setInterval || globalThis.setInterval.bind(globalThis)
  const clearIntervalFn = options.clearInterval || globalThis.clearInterval.bind(globalThis)
  const now = options.now || Date.now
  const maxRecordingMs = options.maxRecordingMs ?? 5 * 60 * 1000
  const recordingStopTimeoutMs = options.recordingStopTimeoutMs ?? 3000
  const constraintsDefaults = options.constraints || {}
  const subscribers = new Set()

  let disposed = false
  let requestToken = 0
  let enumerationToken = 0
  let trackCleanup = null
  let recorderSession = null
  let lastRecorderSession = null
  let elapsedTimer = null
  let maximumTimer = null
  let deviceChangeHandler = null
  let state = {
    status: 'idle',
    stream: null,
    device: null,
    deviceId: null,
    devices: [],
    recording: false,
    recordingElapsed: 0,
    recordingResult: null,
    qualityWarning: null,
    error: null
  }

  const snapshot = () => ({ ...state, devices: state.devices.slice() })
  const publish = (patch) => {
    state = { ...state, ...patch }
    if (disposed) return
    const value = snapshot()
    for (const subscriber of subscribers) {
      try { subscriber(value) } catch { /* subscribers must not break controller operations */ }
    }
  }
  const safeCreateObjectURL = (blob) => {
    try {
      if (!urlApi || typeof urlApi.createObjectURL !== 'function') return { url: null, error: null }
      return { url: urlApi.createObjectURL(blob), error: null }
    } catch (error) {
      return { url: null, error }
    }
  }
  const safeRevokeObjectURL = (url) => {
    if (!url) return null
    try {
      if (urlApi && typeof urlApi.revokeObjectURL === 'function') urlApi.revokeObjectURL(url)
      return null
    } catch (error) {
      return error
    }
  }
  const clearRecordingTimers = () => {
    if (elapsedTimer != null) clearIntervalFn(elapsedTimer)
    if (maximumTimer != null) clearTimeoutFn(maximumTimer)
    elapsedTimer = null
    maximumTimer = null
  }
  const unbindTrack = () => {
    if (trackCleanup) trackCleanup()
    trackCleanup = null
  }
  const releaseRecording = () => {
    const result = state.recordingResult
    const revokeError = safeRevokeObjectURL(result?.url)
    if (result) publish({
      recordingResult: null,
      error: revokeError ? `录像链接释放失败：${getCameraErrorMessage(revokeError)}` : state.error
    })
  }

  async function enumerateDevices({ handleMissing = true } = {}) {
    const token = ++enumerationToken
    const expectedStream = state.stream
    const expectedDeviceId = state.deviceId
    if (!mediaDevices || typeof mediaDevices.enumerateDevices !== 'function') {
      if (!disposed && token === enumerationToken) publish({ devices: [] })
      return []
    }
    try {
      const devices = normalizeVideoDevices(await mediaDevices.enumerateDevices())
      if (disposed || token !== enumerationToken) return devices
      const selected = devices.find((device) => device.deviceId === state.deviceId) || null
      publish({ devices, device: selected || state.device })
      const missingExpectedDevice = expectedStream && expectedDeviceId &&
        !devices.some((device) => device.deviceId === expectedDeviceId)
      if (handleMissing && missingExpectedDevice && token === enumerationToken &&
        state.stream === expectedStream && state.deviceId === expectedDeviceId) {
        await endCurrentStream('当前摄像头已断开', 'ended', expectedStream, expectedDeviceId)
      }
      return devices
    } catch (error) {
      if (!disposed && token === enumerationToken) publish({ error: getCameraErrorMessage(error) })
      return []
    }
  }

  function bindTrack(stream) {
    const track = typeof stream.getVideoTracks === 'function' ? stream.getVideoTracks()[0] : null
    if (!track) return null
    const onEnded = () => {
      if (state.stream === stream && !disposed) void endCurrentStream('摄像头连接已结束')
    }
    if (typeof track.addEventListener === 'function') {
      track.addEventListener('ended', onEnded)
      trackCleanup = () => track.removeEventListener?.('ended', onEnded)
    } else {
      track.onended = onEnded
      trackCleanup = () => {
        if (track.onended === onEnded) track.onended = null
      }
    }
    return track
  }

  function watchNegotiatingTrackEnded(track) {
    let ended = track?.readyState === 'ended'
    if (!track) return { hasEnded: () => false, cleanup: () => {} }
    const onEnded = () => { ended = true }
    if (typeof track.addEventListener === 'function') {
      track.addEventListener('ended', onEnded)
      return {
        hasEnded: () => ended || track.readyState === 'ended',
        cleanup: () => track.removeEventListener?.('ended', onEnded)
      }
    }
    const previous = track.onended
    track.onended = (...args) => {
      ended = true
      if (typeof previous === 'function') previous.apply(track, args)
    }
    return {
      hasEnded: () => ended || track.readyState === 'ended',
      cleanup: () => { track.onended = previous || null }
    }
  }

  async function connect(deviceId = state.deviceId, constraints = {}) {
    if (disposed) throw new Error('摄像头控制器已销毁')
    if (state.recording) throw new Error('录制过程中不能切换摄像头')
    if (!mediaDevices || typeof mediaDevices.getUserMedia !== 'function') {
      const error = new Error('当前环境不支持摄像头访问')
      publish({ status: state.stream ? 'live' : 'error', error: error.message })
      throw error
    }
    const token = ++requestToken
    const previousStream = state.stream
    const previousQualityWarning = state.qualityWarning
    publish({ status: 'connecting', error: null, qualityWarning: null })
    try {
      const stream = await mediaDevices.getUserMedia(buildCameraConstraints({
        ...constraintsDefaults,
        ...constraints,
        deviceId: deviceId || undefined
      }))
      if (disposed || token !== requestToken) {
        stopMediaStream(stream)
        return null
      }
      const track = typeof stream.getVideoTracks === 'function' ? stream.getVideoTracks()[0] : null
      const endedWatch = watchNegotiatingTrackEnded(track)
      const quality = await maximizeVideoTrack(track)
      const negotiationEnded = endedWatch.hasEnded()
      endedWatch.cleanup()
      if (disposed || token !== requestToken || state.stream !== previousStream || negotiationEnded) {
        stopMediaStream(stream)
        if (!disposed && token === requestToken && negotiationEnded) {
          publish({
            status: previousStream ? 'live' : 'ended',
            stream: previousStream,
            qualityWarning: previousQualityWarning,
            error: '摄像头连接已结束'
          })
        }
        return null
      }
      unbindTrack()
      bindTrack(stream)
      const actualDeviceId = deviceId || quality.settings?.deviceId || null
      const device = state.devices.find((item) => item.deviceId === actualDeviceId) || null
      publish({
        status: 'live',
        stream,
        deviceId: actualDeviceId,
        device,
        qualityWarning: quality.warning,
        error: null
      })
      if (previousStream && previousStream !== stream) stopMediaStream(previousStream)
      await enumerateDevices({ handleMissing: false })
      return !disposed && token === requestToken && state.stream === stream ? stream : null
    } catch (error) {
      if (disposed || token !== requestToken) return null
      publish({
        status: previousStream ? 'live' : 'error',
        stream: previousStream,
        qualityWarning: previousQualityWarning,
        error: getCameraErrorMessage(error)
      })
      throw error
    }
  }

  function switchCamera(deviceId, constraints = {}) {
    if (state.recording) return Promise.reject(new Error('录制过程中不能切换摄像头'))
    return connect(deviceId, constraints)
  }

  function finishRecorderSession(session, recorderError = null) {
    if (session.finished) return
    session.finished = true
    if (session.stopTimer != null) clearTimeoutFn(session.stopTimer)
    session.stopTimer = null
    clearRecordingTimers()
    if (recorderSession === session) recorderSession = null
    const mimeType = session.recorder.mimeType || session.mimeType || session.chunks[0]?.type || 'video/webm'
    let blob = null
    try {
      blob = new BlobCtor(session.chunks, { type: mimeType })
    } catch (error) {
      recorderError = recorderError || error
    }
    let result = null
    let urlError = null
    if (!recorderError && blob?.size > 0) {
      const createdUrl = safeCreateObjectURL(blob)
      urlError = createdUrl.error
      result = {
        blob,
        url: createdUrl.url,
        mimeType: blob.type || mimeType,
        extension: recordingExtension(blob.type || mimeType),
        duration: Math.max(0, now() - session.startedAt)
      }
      const oldResult = state.recordingResult
      urlError = safeRevokeObjectURL(oldResult?.url) || urlError
    }
    const message = recorderError
      ? getCameraErrorMessage(recorderError)
      : blob?.size > 0
        ? urlError ? `录像文件已生成，但链接操作失败：${getCameraErrorMessage(urlError)}` : null
        : '录像数据为空，未生成录像文件'
    publish({
      recording: false,
      recordingElapsed: Math.floor(Math.max(0, now() - session.startedAt) / 1000),
      recordingResult: result || state.recordingResult,
      error: message || state.error
    })
    if (recorderError || !blob?.size) session.reject(recorderError || new Error(message))
    else session.resolve(result)
  }

  function startRecording() {
    if (disposed) return Promise.reject(new Error('摄像头控制器已销毁'))
    if (!state.stream || state.status !== 'live') return Promise.reject(new Error('请先连接摄像头'))
    if (state.recording) return Promise.reject(new Error('正在录制中'))
    if (!MediaRecorderCtor) {
      const error = new Error('当前环境不支持视频录制')
      publish({ error: error.message })
      return Promise.reject(error)
    }
    const candidates = options.mimeTypes || RECORDING_MIME_TYPES
    const supportedMimeTypes = typeof MediaRecorderCtor.isTypeSupported === 'function'
      ? candidates.filter((type) => {
          try { return MediaRecorderCtor.isTypeSupported(type) } catch { return false }
        })
      : []
    let recorder
    let mimeType = ''
    let constructionError = null
    for (const candidate of [...supportedMimeTypes, '']) {
      try {
        recorder = candidate
          ? new MediaRecorderCtor(state.stream, { mimeType: candidate })
          : new MediaRecorderCtor(state.stream)
        mimeType = candidate
        break
      } catch (error) {
        constructionError = error
      }
    }
    if (!recorder) {
      publish({ error: getCameraErrorMessage(constructionError) })
      return Promise.reject(constructionError)
    }
    const startedAt = now()
    let resolveStop
    let rejectStop
    const stopped = new Promise((resolve, reject) => {
      resolveStop = resolve
      rejectStop = reject
    })
    // The completion promise may reject before a page calls stopRecording().
    // Keep that rejection observable to callers without creating a global
    // unhandled-rejection in the meantime.
    stopped.catch(() => {})
    const session = {
      recorder,
      mimeType,
      chunks: [],
      startedAt,
      stopped,
      resolve: resolveStop,
      reject: rejectStop,
      finished: false,
      stopRequested: false,
      stopTimer: null,
      error: null
    }
    recorderSession = session
    lastRecorderSession = session
    recorder.ondataavailable = (event) => {
      if (event?.data?.size > 0) session.chunks.push(event.data)
    }
    recorder.onerror = (event) => {
      session.error = event?.error || new Error('录像过程中发生错误')
      publish({ error: getCameraErrorMessage(session.error) })
      requestRecorderStop(session)
    }
    recorder.onstop = () => finishRecorderSession(session, session.error)
    try {
      recorder.start()
    } catch (error) {
      recorderSession = null
      finishRecorderSession(session, error)
      return Promise.reject(error)
    }
    publish({ recording: true, recordingElapsed: 0, error: null })
    elapsedTimer = setIntervalFn(() => {
      if (recorderSession === session) {
        publish({ recordingElapsed: Math.floor(Math.max(0, now() - startedAt) / 1000) })
      }
    }, 1000)
    maximumTimer = setTimeoutFn(() => { void stopRecording().catch(() => null) }, maxRecordingMs)
    return Promise.resolve(snapshot())
  }

  function stopRecording() {
    const session = recorderSession || lastRecorderSession
    if (!session) return Promise.resolve(state.recordingResult)
    requestRecorderStop(session)
    return session.stopped
  }

  function requestRecorderStop(session) {
    if (session.finished || session.stopRequested) return
    session.stopRequested = true
    if (session.recorder.state !== 'inactive') {
      try {
        session.recorder.stop()
      } catch (error) {
        finishRecorderSession(session, error)
        return
      }
    }
    if (session.finished) return
    session.stopTimer = setTimeoutFn(() => {
      finishRecorderSession(session, session.error || new Error('录像停止超时'))
    }, recordingStopTimeoutMs)
  }

  async function endCurrentStream(
    message,
    status = 'ended',
    expectedStream = state.stream,
    expectedDeviceId = state.deviceId
  ) {
    const token = ++requestToken
    await stopRecording().catch(() => null)
    if (token !== requestToken || state.stream !== expectedStream || state.deviceId !== expectedDeviceId) return false
    unbindTrack()
    stopMediaStream(expectedStream)
    publish({
      status,
      stream: null,
      device: null,
      deviceId: null,
      qualityWarning: null,
      error: message || null
    })
    return true
  }

  async function stop() {
    if (disposed) return
    await endCurrentStream(null, 'idle')
  }

  async function dispose() {
    if (disposed) return
    disposed = true
    ++requestToken
    ++enumerationToken
    subscribers.clear()
    if (deviceChangeHandler && mediaDevices?.removeEventListener) {
      mediaDevices.removeEventListener('devicechange', deviceChangeHandler)
    }
    deviceChangeHandler = null
    await stopRecording().catch(() => null)
    clearRecordingTimers()
    unbindTrack()
    stopMediaStream(state.stream)
    const result = state.recordingResult
    safeRevokeObjectURL(result?.url)
    state = {
      ...state,
      status: 'idle',
      stream: null,
      device: null,
      deviceId: null,
      recording: false,
      recordingResult: null,
      qualityWarning: null
    }
  }

  if (mediaDevices?.addEventListener) {
    deviceChangeHandler = () => { void enumerateDevices({ handleMissing: true }) }
    mediaDevices.addEventListener('devicechange', deviceChangeHandler)
  }

  return {
    subscribe(subscriber) {
      if (typeof subscriber !== 'function') throw new TypeError('subscriber 必须是函数')
      if (disposed) return () => false
      subscribers.add(subscriber)
      try { subscriber(snapshot()) } catch { /* isolate initial subscriber notification */ }
      return () => subscribers.delete(subscriber)
    },
    getSnapshot: snapshot,
    enumerateDevices,
    connect,
    switchCamera,
    stop,
    dispose,
    startRecording,
    stopRecording,
    releaseRecording
  }
}
