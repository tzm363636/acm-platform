import { nextTick, ref } from 'vue'
export const confirmation = ref<{ message: string; options: { value: string; label: string }[]; resolve: (value: string) => void; trigger: HTMLElement | null }>()
export function confirmAction(message: string): Promise<boolean> {
  return chooseAction(message, [{ value: 'cancel', label: '取消' }, { value: 'confirm', label: '确认' }]).then(value => value === 'confirm')
}
export function chooseAction(message: string, options: { value: string; label: string }[]): Promise<string> {
  if (confirmation.value) return Promise.resolve('cancel')
  return new Promise(resolve => { confirmation.value = { message, options, resolve, trigger: document.activeElement instanceof HTMLElement ? document.activeElement : null } })
}
export function answerConfirmation(value: string) { const request = confirmation.value; confirmation.value = undefined; request?.resolve(value); void nextTick(() => { const trigger = request?.trigger; const target = trigger?.isConnected && !trigger.matches(':disabled') ? trigger : document.querySelector<HTMLElement>('.account-editor-toolbar button, .account-heading a, .account-nav a'); target?.focus({ preventScroll: true }) }) }
