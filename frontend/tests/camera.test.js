import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildCameraConstraints,
  buildMaximumVideoConstraints,
  createCameraController,
  getCameraErrorMessage,
  maximizeVideoTrack,
  normalizeVideoDevices,
  normalizeCapabilityRange,
  recordingExtension,
  selectRecordingMimeType,
  stopMediaStream
} from '../src/utils/camera.js'

const deferred = () => {
  let resolve
  let reject
  const promise = new Promise((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

function makeTrack(deviceId = 'camera-1') {
  const listeners = new Map()
  return {
    stopped: 0,
    stop() { this.stopped += 1 },
    getSettings() { return { deviceId } },
    addEventListener(type, fn) { listeners.set(type, fn) },
    removeEventListener(type, fn) { if (listeners.get(type) === fn) listeners.delete(type) },
    emit(type) { listeners.get(type)?.() },
    listenerCount() { return listeners.size }
  }
}

function makeStream(deviceId = 'camera-1') {
  const track = makeTrack(deviceId)
  return {
    track,
    getTracks: () => [track],
    getVideoTracks: () => [track]
  }
}

function makeNegotiatingStream(deviceId, negotiation) {
  const stream = makeStream(deviceId)
  negotiation.started ||= deferred()
  stream.track.readyState = 'live'
  stream.track.getCapabilities = () => ({
    width: { min: 640, max: 1920 },
    height: { min: 480, max: 1080 },
    frameRate: { min: 1, max: 60 }
  })
  stream.track.applyConstraints = () => {
    negotiation.started.resolve()
    return negotiation.promise
  }
  return stream
}

function makeMediaDevices({ streams = [], devices = [] } = {}) {
  const listeners = new Map()
  let call = 0
  return {
    requests: [],
    getUserMedia(constraints) {
      this.requests.push(constraints)
      const value = streams[call++]
      return value?.promise || Promise.resolve(value)
    },
    enumerateDevices: async () => devices,
    addEventListener(type, fn) { listeners.set(type, fn) },
    removeEventListener(type, fn) { if (listeners.get(type) === fn) listeners.delete(type) },
    emit(type) { listeners.get(type)?.() },
    listenerCount() { return listeners.size }
  }
}

class FakeRecorder {
  static supported = ['video/webm']
  static instances = []
  static isTypeSupported(type) { return this.supported.includes(type) }
  constructor(stream, options) {
    this.stream = stream
    this.options = options
    this.mimeType = options?.mimeType || 'video/webm'
    this.state = 'inactive'
    FakeRecorder.instances.push(this)
  }
  start() { this.state = 'recording' }
  stop() { this.state = 'inactive' }
  data(value, type = this.mimeType) { this.ondataavailable?.({ data: new Blob([value], { type }) }) }
  finish() { this.onstop?.() }
}

test('生成 audio:false、ideal 分辨率和 exact 设备约束', () => {
  assert.deepEqual(buildCameraConstraints({ deviceId: 'cam', width: 1920, height: 1080, frameRate: 30 }), {
    audio: false,
    video: {
      width: { ideal: 1920 },
      height: { ideal: 1080 },
      frameRate: { ideal: 30 },
      deviceId: { exact: 'cam' }
    }
  })
  assert.deepEqual(buildCameraConstraints(), { audio: false, video: {} })
  assert.deepEqual(buildCameraConstraints({ deviceId: 'only-device' }), {
    audio: false,
    video: { deviceId: { exact: 'only-device' } }
  })
})

test('校验画质能力范围并生成最高分辨率优先约束', () => {
  assert.deepEqual(normalizeCapabilityRange({ min: 320, max: 3840 }), { min: 320, max: 3840 })
  assert.equal(normalizeCapabilityRange({ min: 0, max: 1920 }), null)
  assert.equal(normalizeCapabilityRange({ min: 60, max: 30 }), null)
  assert.deepEqual(buildMaximumVideoConstraints({
    width: { min: 320, max: 3840 },
    height: { min: 240, max: 2160 },
    frameRate: { min: 1, max: 60 }
  }), {
    width: { ideal: 3840 },
    height: { ideal: 2160 },
    frameRate: { ideal: 30, max: 30 }
  })
  assert.deepEqual(buildMaximumVideoConstraints({ frameRate: { min: 1, max: 24 } }), {
    frameRate: { ideal: 24, max: 24 }
  })
})

test('maximizeVideoTrack 在 capability API 缺失时正常降级', async () => {
  const track = makeTrack('a')
  assert.deepEqual(await maximizeVideoTrack(track), {
    settings: { deviceId: 'a' },
    constraints: null,
    warning: null
  })
})

test('maximizeVideoTrack 应用最佳约束并返回真实 settings', async () => {
  let applied
  const track = {
    getCapabilities: () => ({
      width: { min: 640, max: 1920 },
      height: { min: 480, max: 1080 },
      frameRate: { min: 1, max: 60 }
    }),
    applyConstraints: async (constraints) => { applied = constraints },
    getSettings: () => ({ width: 1920, height: 1080, frameRate: 30 })
  }
  const result = await maximizeVideoTrack(track)
  assert.deepEqual(applied, {
    width: { ideal: 1920 },
    height: { ideal: 1080 },
    frameRate: { ideal: 30, max: 30 }
  })
  assert.deepEqual(result.settings, { width: 1920, height: 1080, frameRate: 30 })
  assert.equal(result.warning, null)
})

test('maximizeVideoTrack 应用失败时返回非致命 warning 和基础 settings', async () => {
  const track = {
    getCapabilities: () => ({ width: { min: 640, max: 1920 } }),
    applyConstraints: async () => { throw new Error('协商失败') },
    getSettings: () => ({ width: 640 })
  }
  const result = await maximizeVideoTrack(track)
  assert.deepEqual(result.settings, { width: 640 })
  assert.match(result.warning, /保留基础视频流.*协商失败/)
})

test('规范化摄像头并为无标签设备生成中文标签', () => {
  assert.deepEqual(normalizeVideoDevices([
    { kind: 'audioinput', deviceId: 'mic' },
    { kind: 'videoinput', deviceId: 'a', label: '' },
    { kind: 'videoinput', deviceId: 'b', label: '后置镜头', groupId: 'g' }
  ]), [
    { kind: 'videoinput', deviceId: 'a', groupId: '', label: '摄像头 1' },
    { kind: 'videoinput', deviceId: 'b', groupId: 'g', label: '后置镜头' }
  ])
})

test('映射摄像头错误为中文', () => {
  assert.match(getCameraErrorMessage({ name: 'NotAllowedError' }), /权限被拒绝/)
  assert.equal(getCameraErrorMessage({ name: 'Other', message: '自定义错误' }), '自定义错误')
})

test('停止流中的全部轨道', () => {
  const tracks = [makeTrack(), makeTrack()]
  stopMediaStream({ getTracks: () => tracks })
  assert.deepEqual(tracks.map((track) => track.stopped), [1, 1])
})

test('选择受支持 MIME 且扩展名匹配', () => {
  class Recorder { static isTypeSupported(type) { return type === 'video/mp4' } }
  assert.equal(selectRecordingMimeType(Recorder, ['video/webm', 'video/mp4']), 'video/mp4')
  assert.equal(selectRecordingMimeType(null), '')
  assert.equal(recordingExtension('video/webm;codecs=vp9'), 'webm')
  assert.equal(recordingExtension('video/mp4'), 'mp4')
})

test('旧连接请求晚返回时立即释放旧请求流', async () => {
  const first = deferred()
  const second = deferred()
  const mediaDevices = makeMediaDevices({ streams: [first, second] })
  const controller = createCameraController({ mediaDevices })
  const p1 = controller.connect('a')
  const p2 = controller.connect('b')
  const current = makeStream('b')
  second.resolve(current)
  await p2
  const stale = makeStream('a')
  first.resolve(stale)
  assert.equal(await p1, null)
  assert.equal(stale.track.stopped, 1)
  assert.equal(controller.getSnapshot().stream, current)
  await controller.dispose()
})

test('dispose 后返回的连接流会被释放', async () => {
  const pending = deferred()
  const controller = createCameraController({ mediaDevices: makeMediaDevices({ streams: [pending] }) })
  const connecting = controller.connect('a')
  await controller.dispose()
  const stream = makeStream('a')
  pending.resolve(stream)
  assert.equal(await connecting, null)
  assert.equal(stream.track.stopped, 1)
})

test('成功换流后才释放旧流并清理旧 ended 监听', async () => {
  const oldStream = makeStream('a')
  const next = deferred()
  const controller = createCameraController({ mediaDevices: makeMediaDevices({ streams: [oldStream, next] }) })
  await controller.connect('a')
  const switching = controller.switchCamera('b')
  assert.equal(oldStream.track.stopped, 0)
  const newStream = makeStream('b')
  next.resolve(newStream)
  await switching
  assert.equal(oldStream.track.stopped, 1)
  assert.equal(oldStream.track.listenerCount(), 0)
  assert.equal(controller.getSnapshot().stream, newStream)
  await controller.dispose()
})

test('换流失败保留旧流和 live 状态', async () => {
  const oldStream = makeStream('a')
  const failed = deferred()
  const controller = createCameraController({ mediaDevices: makeMediaDevices({ streams: [oldStream, failed] }) })
  await controller.connect('a')
  const switching = controller.switchCamera('b', { width: 1920, height: 1080, frameRate: 60 })
  failed.reject(Object.assign(new Error('占用'), { name: 'NotReadableError' }))
  await assert.rejects(switching)
  assert.equal(oldStream.track.stopped, 0)
  assert.equal(controller.getSnapshot().stream, oldStream)
  assert.equal(controller.getSnapshot().status, 'live')
  assert.deepEqual(controller.getSnapshot().stream, oldStream)
  await controller.dispose()
})

test('connect 和 switchCamera 合并每次调用的约束覆盖', async () => {
  const first = makeStream('a')
  const second = makeStream('b')
  const mediaDevices = makeMediaDevices({ streams: [first, second] })
  const controller = createCameraController({
    mediaDevices,
    constraints: { width: 640, height: 480, frameRate: 15 }
  })
  await controller.connect('a', { width: 1920, height: 1080, frameRate: 30 })
  assert.deepEqual(mediaDevices.requests[0], {
    audio: false,
    video: {
      width: { ideal: 1920 },
      height: { ideal: 1080 },
      frameRate: { ideal: 30 },
      deviceId: { exact: 'a' }
    }
  })
  await controller.switchCamera('b', { width: 1280, height: 720, frameRate: 60 })
  assert.deepEqual(mediaDevices.requests[1].video, {
    width: { ideal: 1280 },
    height: { ideal: 720 },
    frameRate: { ideal: 60 },
    deviceId: { exact: 'b' }
  })
  assert.equal(first.track.stopped, 1)
  await controller.dispose()
})

test('当前轨道 ended 后进入 ended 并释放流', async () => {
  const stream = makeStream('a')
  const controller = createCameraController({ mediaDevices: makeMediaDevices({ streams: [stream] }) })
  await controller.connect('a')
  stream.track.emit('ended')
  await new Promise((resolve) => setTimeout(resolve, 0))
  assert.equal(controller.getSnapshot().status, 'ended')
  assert.equal(controller.getSnapshot().stream, null)
  assert.equal(stream.track.stopped, 1)
  await controller.dispose()
})

test('devicechange 重新枚举、处理设备消失并在 dispose 清理监听', async () => {
  const stream = makeStream('a')
  const devices = [{ kind: 'videoinput', deviceId: 'a', label: 'A' }]
  const mediaDevices = makeMediaDevices({ streams: [stream], devices })
  const controller = createCameraController({ mediaDevices })
  assert.equal(mediaDevices.listenerCount(), 1)
  await controller.connect('a')
  devices.splice(0)
  mediaDevices.emit('devicechange')
  await new Promise((resolve) => setTimeout(resolve, 0))
  assert.equal(controller.getSnapshot().status, 'ended')
  await controller.dispose()
  assert.equal(mediaDevices.listenerCount(), 0)
})

test('stop 等待最终 dataavailable 后再生成录像', async () => {
  FakeRecorder.instances = []
  const stream = makeStream()
  const urls = []
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [stream] }),
    MediaRecorderCtor: FakeRecorder,
    urlApi: { createObjectURL: (blob) => (urls.push(blob), 'blob:1'), revokeObjectURL() {} }
  })
  await controller.connect()
  await controller.startRecording()
  const recorder = FakeRecorder.instances.at(-1)
  const stopping = controller.stopRecording()
  let settled = false
  stopping.then(() => { settled = true })
  await Promise.resolve()
  assert.equal(settled, false)
  recorder.data('final')
  recorder.finish()
  const result = await stopping
  assert.equal(await result.blob.text(), 'final')
  assert.equal(urls.length, 1)
  await controller.dispose()
})

test('重复 stopRecording 幂等并返回同一个完成 Promise', async () => {
  FakeRecorder.instances = []
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [makeStream()] }),
    MediaRecorderCtor: FakeRecorder
  })
  await controller.connect()
  await controller.startRecording()
  const recorder = FakeRecorder.instances.at(-1)
  const first = controller.stopRecording()
  const second = controller.stopRecording()
  assert.equal(first, second)
  recorder.data('tail')
  recorder.finish()
  assert.equal(await (await first).blob.text(), 'tail')
  assert.equal(controller.stopRecording(), first)
  await controller.dispose()
})

test('stop 后立即 dispose 仍等待最终录像数据', async () => {
  FakeRecorder.instances = []
  const stream = makeStream()
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [stream] }),
    MediaRecorderCtor: FakeRecorder
  })
  await controller.connect()
  await controller.startRecording()
  const recorder = FakeRecorder.instances.at(-1)
  const stopping = controller.stopRecording()
  const disposing = controller.dispose()
  assert.equal(stream.track.stopped, 0)
  recorder.data('last-frame')
  recorder.finish()
  assert.equal(await (await stopping).blob.text(), 'last-frame')
  await disposing
  assert.equal(stream.track.stopped, 1)
})

test('track ended 在录制时等待录像 onstop 后再释放流', async () => {
  FakeRecorder.instances = []
  const stream = makeStream('a')
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [stream] }),
    MediaRecorderCtor: FakeRecorder
  })
  await controller.connect('a')
  await controller.startRecording()
  const recorder = FakeRecorder.instances.at(-1)
  stream.track.emit('ended')
  await Promise.resolve()
  assert.equal(stream.track.stopped, 0)
  recorder.data('ended-tail')
  recorder.finish()
  await new Promise((resolve) => setTimeout(resolve, 0))
  assert.equal(controller.getSnapshot().status, 'ended')
  assert.equal(stream.track.stopped, 1)
  await controller.dispose()
})

test('录像 onerror 等待 onstop 并以错误结束', async () => {
  FakeRecorder.instances = []
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [makeStream()] }),
    MediaRecorderCtor: FakeRecorder
  })
  await controller.connect()
  await controller.startRecording()
  const recorder = FakeRecorder.instances.at(-1)
  const failure = new Error('编码失败')
  recorder.onerror({ error: failure })
  const stopping = controller.stopRecording()
  let settled = false
  stopping.catch(() => { settled = true })
  await Promise.resolve()
  assert.equal(settled, false)
  recorder.data('ignored')
  recorder.finish()
  await assert.rejects(stopping, /编码失败/)
  assert.equal(controller.getSnapshot().recording, false)
  await controller.dispose()
})

test('录像停止事件超时后有限兜底并清理停止定时器', async () => {
  FakeRecorder.instances = []
  let stopTimeout
  const cleared = []
  let timerId = 0
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [makeStream()] }),
    MediaRecorderCtor: FakeRecorder,
    maxRecordingMs: 1000,
    recordingStopTimeoutMs: 25,
    setTimeout: (fn, delay) => {
      const id = ++timerId
      if (delay === 25) stopTimeout = fn
      return id
    },
    clearTimeout: (id) => cleared.push(id),
    setInterval: () => 10,
    clearInterval: (id) => cleared.push(id)
  })
  await controller.connect()
  await controller.startRecording()
  const stopping = controller.stopRecording()
  assert.equal(typeof stopTimeout, 'function')
  stopTimeout()
  await assert.rejects(stopping, /录像停止超时/)
  assert.equal(controller.getSnapshot().recording, false)
  assert.ok(cleared.length >= 3)
  await controller.dispose()
})

test('录制时禁止切换摄像头', async () => {
  FakeRecorder.instances = []
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [makeStream()] }),
    MediaRecorderCtor: FakeRecorder
  })
  await controller.connect()
  await controller.startRecording()
  await assert.rejects(controller.switchCamera('b'), /录制过程中/)
  const recorder = FakeRecorder.instances.at(-1)
  recorder.data('x')
  const stopping = controller.stopRecording()
  recorder.finish()
  await stopping
  await controller.dispose()
})

test('达到最大时长自动调用 recorder.stop 并清理定时器', async () => {
  FakeRecorder.instances = []
  let maximumCallback
  const cleared = []
  let timerId = 8
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [makeStream()] }),
    MediaRecorderCtor: FakeRecorder,
    maxRecordingMs: 123,
    setTimeout: (fn, delay) => {
      const id = ++timerId
      if (delay === 123) maximumCallback = fn
      return id
    },
    clearTimeout: (id) => cleared.push(id),
    setInterval: () => 8,
    clearInterval: (id) => cleared.push(id)
  })
  await controller.connect()
  await controller.startRecording()
  const recorder = FakeRecorder.instances.at(-1)
  maximumCallback()
  assert.equal(recorder.state, 'inactive')
  recorder.data('last')
  recorder.finish()
  await Promise.resolve()
  assert.deepEqual(cleared.sort((a, b) => a - b), [8, 9, 10])
  await controller.dispose()
})

test('新录像替换旧 URL，release 和 dispose 均撤销 URL', async () => {
  FakeRecorder.instances = []
  const revoked = []
  let sequence = 0
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [makeStream()] }),
    MediaRecorderCtor: FakeRecorder,
    urlApi: {
      createObjectURL: () => `blob:${++sequence}`,
      revokeObjectURL: (url) => revoked.push(url)
    }
  })
  await controller.connect()
  await controller.startRecording()
  let recorder = FakeRecorder.instances.at(-1)
  recorder.data('one')
  let stopping = controller.stopRecording()
  recorder.finish()
  await stopping
  await controller.startRecording()
  recorder = FakeRecorder.instances.at(-1)
  recorder.data('two')
  stopping = controller.stopRecording()
  recorder.finish()
  await stopping
  assert.deepEqual(revoked, ['blob:1'])
  controller.releaseRecording()
  assert.deepEqual(revoked, ['blob:1', 'blob:2'])
  await controller.startRecording()
  recorder = FakeRecorder.instances.at(-1)
  recorder.data('three')
  stopping = controller.stopRecording()
  recorder.finish()
  await stopping
  await controller.dispose()
  assert.deepEqual(revoked, ['blob:1', 'blob:2', 'blob:3'])
})

test('createObjectURL 抛错时 stopRecording 仍完成并保留可下载 Blob', async () => {
  FakeRecorder.instances = []
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [makeStream()] }),
    MediaRecorderCtor: FakeRecorder,
    urlApi: { createObjectURL() { throw new Error('URL 创建失败') } }
  })
  await controller.connect()
  await controller.startRecording()
  const recorder = FakeRecorder.instances.at(-1)
  recorder.data('downloadable')
  const stopping = controller.stopRecording()
  recorder.finish()
  const result = await stopping
  assert.equal(result.url, null)
  assert.equal(await result.blob.text(), 'downloadable')
  assert.match(controller.getSnapshot().error, /URL 创建失败/)
  await controller.dispose()
})

test('revokeObjectURL 抛错不阻止录像替换、释放和 dispose', async () => {
  FakeRecorder.instances = []
  const stream = makeStream()
  let sequence = 0
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [stream] }),
    MediaRecorderCtor: FakeRecorder,
    urlApi: {
      createObjectURL: () => `blob:${++sequence}`,
      revokeObjectURL() { throw new Error('URL 释放失败') }
    }
  })
  await controller.connect()
  for (const data of ['first', 'second']) {
    await controller.startRecording()
    const recorder = FakeRecorder.instances.at(-1)
    recorder.data(data)
    const stopping = controller.stopRecording()
    recorder.finish()
    await stopping
  }
  assert.equal(controller.getSnapshot().recordingResult.url, 'blob:2')
  controller.releaseRecording()
  assert.equal(controller.getSnapshot().recordingResult, null)
  await controller.startRecording()
  const recorder = FakeRecorder.instances.at(-1)
  recorder.data('third')
  const stopping = controller.stopRecording()
  recorder.finish()
  await stopping
  await controller.dispose()
  assert.equal(stream.track.stopped, 1)
  assert.equal(controller.getSnapshot().recordingResult, null)
})

test('connect 枚举等待期间 stop 后返回 null', async () => {
  const enumeration = deferred()
  const enumerationStarted = deferred()
  const stream = makeStream('a')
  const mediaDevices = makeMediaDevices({ streams: [stream] })
  mediaDevices.enumerateDevices = () => {
    enumerationStarted.resolve()
    return enumeration.promise
  }
  const controller = createCameraController({ mediaDevices })
  const connecting = controller.connect('a')
  await enumerationStarted.promise
  await controller.stop()
  enumeration.resolve([{ kind: 'videoinput', deviceId: 'a', label: 'A' }])
  assert.equal(await connecting, null)
  assert.equal(stream.track.stopped, 1)
  await controller.dispose()
})

test('下一次连接发生在旧枚举等待期间时旧 connect 返回 null', async () => {
  const firstEnumeration = deferred()
  const secondEnumeration = deferred()
  const firstEnumerationStarted = deferred()
  const secondEnumerationStarted = deferred()
  const firstStream = makeStream('a')
  const secondStream = makeStream('b')
  const mediaDevices = makeMediaDevices({ streams: [firstStream, secondStream] })
  let enumerationCall = 0
  mediaDevices.enumerateDevices = () => {
    const index = enumerationCall++
    ;[firstEnumerationStarted, secondEnumerationStarted][index].resolve()
    return [firstEnumeration, secondEnumeration][index].promise
  }
  const controller = createCameraController({ mediaDevices })
  const firstConnect = controller.connect('a')
  await firstEnumerationStarted.promise
  const secondConnect = controller.connect('b')
  await secondEnumerationStarted.promise
  secondEnumeration.resolve([{ kind: 'videoinput', deviceId: 'b', label: 'B' }])
  assert.equal(await secondConnect, secondStream)
  firstEnumeration.resolve([{ kind: 'videoinput', deviceId: 'a', label: 'A' }])
  assert.equal(await firstConnect, null)
  assert.equal(controller.getSnapshot().stream, secondStream)
  await controller.dispose()
})

test('旧设备枚举晚返回不会覆盖新列表或断开新流', async () => {
  const stream = makeStream('b')
  const mediaDevices = makeMediaDevices({
    streams: [stream],
    devices: [{ kind: 'videoinput', deviceId: 'b', label: 'B' }]
  })
  const controller = createCameraController({ mediaDevices })
  await controller.connect('b')
  const oldEnumeration = deferred()
  const newEnumeration = deferred()
  let call = 0
  mediaDevices.enumerateDevices = () => [oldEnumeration, newEnumeration][call++].promise
  const oldRequest = controller.enumerateDevices({ handleMissing: true })
  const newRequest = controller.enumerateDevices({ handleMissing: true })
  newEnumeration.resolve([{ kind: 'videoinput', deviceId: 'b', label: '新设备 B' }])
  await newRequest
  oldEnumeration.resolve([])
  await oldRequest
  assert.deepEqual(controller.getSnapshot().devices.map((device) => device.label), ['新设备 B'])
  assert.equal(controller.getSnapshot().stream, stream)
  assert.equal(controller.getSnapshot().status, 'live')
  await controller.dispose()
})

test('订阅者异常不会中断连接，dispose 开始后不再通知订阅者', async () => {
  const stream = makeStream()
  const controller = createCameraController({ mediaDevices: makeMediaDevices({ streams: [stream] }) })
  let notifications = 0
  controller.subscribe(() => { throw new Error('订阅者异常') })
  controller.subscribe(() => { notifications += 1 })
  await controller.connect()
  assert.equal(controller.getSnapshot().stream, stream)
  const beforeDispose = notifications
  await controller.dispose()
  assert.equal(notifications, beforeDispose)
})

test('MediaRecorder MIME 构造失败时尝试后续候选及无 mimeType 降级', async () => {
  class FallbackRecorder extends FakeRecorder {
    static attempts = []
    static isTypeSupported() { return true }
    constructor(stream, options) {
      FallbackRecorder.attempts.push(options?.mimeType || '')
      if (options?.mimeType) throw new Error('该 MIME 构造失败')
      super(stream, options)
    }
  }
  FallbackRecorder.attempts = []
  FakeRecorder.instances = []
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [makeStream()] }),
    MediaRecorderCtor: FallbackRecorder,
    mimeTypes: ['video/first', 'video/second']
  })
  await controller.connect()
  await controller.startRecording()
  assert.deepEqual(FallbackRecorder.attempts, ['video/first', 'video/second', ''])
  const recorder = FakeRecorder.instances.at(-1)
  recorder.data('fallback')
  const stopping = controller.stopRecording()
  recorder.finish()
  await stopping
  await controller.dispose()
})

test('controller 画质应用失败仍发布基础流和 qualityWarning', async () => {
  const stream = makeStream('quality-fallback')
  stream.track.getCapabilities = () => ({ width: { min: 640, max: 1920 } })
  stream.track.applyConstraints = async () => { throw new Error('设备拒绝约束') }
  const controller = createCameraController({ mediaDevices: makeMediaDevices({ streams: [stream] }) })
  assert.equal(await controller.connect('quality-fallback'), stream)
  assert.equal(controller.getSnapshot().stream, stream)
  assert.match(controller.getSnapshot().qualityWarning, /保留基础视频流.*设备拒绝约束/)
  await controller.dispose()
})

test('最佳画质协商 pending 时 stop 使新流过期并停止', async () => {
  const negotiation = deferred()
  const stream = makeNegotiatingStream('a', negotiation)
  const controller = createCameraController({ mediaDevices: makeMediaDevices({ streams: [stream] }) })
  const connecting = controller.connect('a')
  await negotiation.started.promise
  await controller.stop()
  negotiation.resolve()
  assert.equal(await connecting, null)
  assert.equal(stream.track.stopped, 1)
  assert.equal(controller.getSnapshot().status, 'idle')
  assert.equal(controller.getSnapshot().stream, null)
  await controller.dispose()
})

test('下一次 switch 在最佳画质协商期间使旧请求流过期', async () => {
  const negotiation = deferred()
  const pendingStream = makeNegotiatingStream('a', negotiation)
  const currentStream = makeStream('b')
  const mediaDevices = makeMediaDevices({ streams: [pendingStream, currentStream] })
  const controller = createCameraController({ mediaDevices })
  const firstConnect = controller.connect('a')
  await negotiation.started.promise
  const switching = controller.switchCamera('b')
  assert.equal(await switching, currentStream)
  negotiation.resolve()
  assert.equal(await firstConnect, null)
  assert.equal(pendingStream.track.stopped, 1)
  assert.equal(controller.getSnapshot().stream, currentStream)
  await controller.dispose()
})

test('最佳画质协商完成前保留旧流，完成后才原子换流', async () => {
  const oldStream = makeStream('old')
  const negotiation = deferred()
  const newStream = makeNegotiatingStream('new', negotiation)
  const controller = createCameraController({
    mediaDevices: makeMediaDevices({ streams: [oldStream, newStream] })
  })
  await controller.connect('old')
  const switching = controller.switchCamera('new')
  await negotiation.started.promise
  assert.equal(oldStream.track.stopped, 0)
  assert.equal(controller.getSnapshot().stream, oldStream)
  negotiation.resolve()
  assert.equal(await switching, newStream)
  assert.equal(oldStream.track.stopped, 1)
  assert.equal(controller.getSnapshot().stream, newStream)
  await controller.dispose()
})

test('最佳画质协商 pending 时 dispose 最终释放新流', async () => {
  const negotiation = deferred()
  const stream = makeNegotiatingStream('a', negotiation)
  const controller = createCameraController({ mediaDevices: makeMediaDevices({ streams: [stream] }) })
  const connecting = controller.connect('a')
  await negotiation.started.promise
  const disposing = controller.dispose()
  negotiation.resolve()
  assert.equal(await connecting, null)
  await disposing
  assert.equal(stream.track.stopped, 1)
  assert.equal(controller.getSnapshot().stream, null)
})

test('最佳画质协商期间 track ended 不会发布已结束流', async () => {
  const negotiation = deferred()
  const stream = makeNegotiatingStream('a', negotiation)
  const controller = createCameraController({ mediaDevices: makeMediaDevices({ streams: [stream] }) })
  const snapshots = []
  controller.subscribe((snapshot) => snapshots.push(snapshot))
  const connecting = controller.connect('a')
  await negotiation.started.promise
  stream.track.readyState = 'ended'
  stream.track.emit('ended')
  negotiation.resolve()
  assert.equal(await connecting, null)
  assert.equal(stream.track.stopped, 1)
  assert.equal(controller.getSnapshot().status, 'ended')
  assert.equal(controller.getSnapshot().stream, null)
  assert.equal(snapshots.some((snapshot) => snapshot.stream === stream), false)
  await controller.dispose()
})
