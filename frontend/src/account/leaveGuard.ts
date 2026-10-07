type Guard = () => Promise<boolean>
let guard: Guard | undefined
let pending: Promise<boolean> | undefined
export function registerLeaveGuard(value: Guard) { guard = value; return () => { if (guard === value) guard = undefined } }
export function requestLeave() {
  if (!guard) return Promise.resolve(true)
  return pending ||= guard().finally(() => { pending = undefined })
}
