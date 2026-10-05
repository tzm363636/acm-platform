// Vue keeps leaving elements mounted until their transition ends.
export function concealLeavingSurface(element: Element) {
  element.setAttribute('inert', '')
  element.setAttribute('aria-hidden', 'true')
}
