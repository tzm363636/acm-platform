export type SaveState = 'unsaved' | 'saving' | 'saved' | 'failed'
export interface SaveOptions<S, R> {
  dirty(): boolean
  autoAllowed(): boolean
  capture(): S | undefined
  send(snapshot: S): Promise<R>
  accept(result: R, snapshot: S): void
  report(state: SaveState, error?: unknown): void
  failed?(error: unknown): void
  delay?: number
  schedule?: (callback: () => void, delay: number) => unknown
  cancel?: (timer: unknown) => void
}

/** One writer per editor. A response only acknowledges its captured snapshot. */
export class DraftSaver<S, R> {
  private timer: unknown
  private pending?: Promise<boolean>
  private paused = false
  private disposed = false
  constructor(private options: SaveOptions<S, R>) {}
  private cancelTimer() {
    if (this.timer !== undefined) (this.options.cancel || (timer => clearTimeout(timer as ReturnType<typeof setTimeout>)))(this.timer)
    this.timer = undefined
  }
  edited() {
    this.cancelTimer()
    if (this.disposed) return
    if (!this.options.dirty()) { if (!this.pending) this.options.report('saved'); return }
    if (!this.pending && !this.paused) this.options.report('unsaved')
    if (!this.paused && this.options.autoAllowed()) this.timer = (this.options.schedule || setTimeout)(() => { this.timer = undefined; void this.saveOnce() }, this.options.delay ?? 1200)
  }
  private saveOnce(): Promise<boolean> {
    this.cancelTimer()
    if (this.disposed || this.paused) return Promise.resolve(false)
    if (this.pending) return this.pending
    if (!this.options.dirty()) return Promise.resolve(true)
    const snapshot = this.options.capture()
    if (!snapshot) { this.options.report('unsaved'); return Promise.resolve(false) }
    this.options.report('saving')
    this.pending = Promise.resolve().then(() => this.options.send(snapshot)).then(result => {
      if (this.disposed) return false
      this.options.accept(result, snapshot)
      this.options.report(this.options.dirty() ? 'unsaved' : 'saved')
      return true
    }).catch(error => {
      if (!this.disposed) { this.paused = true; this.options.report('failed', error); this.options.failed?.(error) }
      return false
    }).finally(() => {
      this.pending = undefined
      if (!this.disposed && !this.paused && this.options.dirty()) this.edited()
    })
    return this.pending
  }
  async flush(): Promise<boolean> {
    this.cancelTimer()
    if (this.disposed) return false
    this.paused = false
    if (this.pending && !await this.pending) return false
    while (this.options.dirty()) { if (!await this.saveOnce()) return false; this.cancelTimer() }
    return true
  }
  pause() { this.paused = true; this.cancelTimer() }
  reset() { this.paused = false; this.cancelTimer(); this.options.report(this.options.dirty() ? 'unsaved' : 'saved') }
  dispose() { this.disposed = true; this.cancelTimer() }
}
