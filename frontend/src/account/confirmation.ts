import { nextTick, ref } from 'vue'
export const confirmation = ref<{ message: string; resolve: (value: boolean) => void; trigger: HTMLElement | null }>()
export function confirmAction(message: string): Promise<boolean> {
  if (confirmation.value) return Promise.resolve(false)
  return new Promise(resolve => { confirmation.value = { message, resolve, trigger: document.activeElement instanceof HTMLElement ? document.activeElement : null } })
}
export function answerConfirmation(value: boolean) { const request = confirmation.value; confirmation.value = undefined; request?.resolve(value); void nextTick(() => { const trigger = request?.trigger; const target = trigger?.isConnected && !trigger.matches(':disabled') ? trigger : document.querySelector<HTMLElement>('.account-editor-toolbar button, .account-heading a, .account-nav a'); target?.focus({ preventScroll: true }) }) }
