import { nextTick, onBeforeUnmount, onMounted, ref, watch, type Ref } from 'vue'

export type ReadingHeading = { id: string; text: string; level: number }

export function readingProgress(bodyTop: number, bodyBottom: number, scrollTop: number, viewportHeight: number, offset: number) {
  const travel = bodyBottom - bodyTop - (viewportHeight - offset)
  if (travel <= 0) return bodyBottom <= scrollTop + viewportHeight ? 100 : 0
  return Math.min(100, Math.max(0, ((scrollTop + offset - bodyTop) / travel) * 100))
}

export function collectReadingHeadings(body: HTMLElement): ReadingHeading[] {
  const elements = Array.from(body.querySelectorAll<HTMLElement>('h2, h3, h4'))
  const reservedIds = new Set(Array.from(body.ownerDocument.querySelectorAll<HTMLElement>('[id]'))
    .filter(element => !elements.includes(element)).map(element => element.id))

  return elements.map((element, index) => {
    let id = element.id
    if (!id || reservedIds.has(id)) {
      const base = `article-heading-${index + 1}`
      id = base
      for (let suffix = 2; reservedIds.has(id); suffix++) id = `${base}-${suffix}`
      element.id = id
    }
    reservedIds.add(id)
    element.tabIndex = -1
    return { id, text: element.textContent?.trim() || '未命名章节', level: Number(element.tagName.slice(1)) }
  })
}

export function useArticleReading(body: Ref<HTMLElement | null>, toc: Ref<HTMLElement | null>) {
  const headings = ref<ReadingHeading[]>([])
  const activeHeading = ref('')
  const progress = ref(0)
  const tocExpanded = ref(false)
  const headerHeight = ref(72)
  const scrollOffset = ref(88)
  let header: HTMLElement | null = null
  let headingElements: HTMLElement[] = []
  let frame = 0
  let resizeObserver: ResizeObserver | undefined
  let mutationObserver: MutationObserver | undefined

  function updateReadingPosition() {
    if (!body.value) return
    headerHeight.value = header?.getBoundingClientRect().height || 72
    const mobileTocHeight = window.matchMedia('(max-width: 1000px)').matches
      ? toc.value?.getBoundingClientRect().height || 0 : 0
    scrollOffset.value = headerHeight.value + 16 + (mobileTocHeight ? mobileTocHeight + 12 : 0)
    const bounds = body.value.getBoundingClientRect()
    const firstContent = body.value.firstElementChild?.getBoundingClientRect()
    const lastContent = body.value.lastElementChild?.getBoundingClientRect()
    progress.value = firstContent ? readingProgress(firstContent.top + window.scrollY,
      (lastContent?.bottom ?? bounds.bottom) + window.scrollY, window.scrollY, window.innerHeight, scrollOffset.value) : 0

    let current: HTMLElement | undefined = headingElements[0]
    for (const element of headingElements) {
      if (element.getBoundingClientRect().top > scrollOffset.value + 2) break
      current = element
    }
    if (progress.value >= 100) current = headingElements.at(-1)
    activeHeading.value = current?.id || ''
  }

  function queueUpdate() {
    if (frame) return
    frame = window.requestAnimationFrame(() => {
      frame = 0
      updateReadingPosition()
    })
  }

  function refreshHeadings() {
    if (!body.value) return
    headings.value = collectReadingHeadings(body.value)
    headingElements = headings.value.map(heading => body.value!.ownerDocument.getElementById(heading.id)!)
    queueUpdate()
  }

  async function goToHeading(id: string, smooth = true) {
    tocExpanded.value = false
    await nextTick()
    updateReadingPosition()
    const element = headingElements.find(heading => heading.id === id)
    if (!element) return
    const top = Math.max(0, element.getBoundingClientRect().top + window.scrollY - scrollOffset.value)
    const url = new URL(window.location.href)
    url.hash = id
    window.history.replaceState(window.history.state, '', url)
    element.focus({ preventScroll: true })
    window.scrollTo({ top, behavior: smooth && !window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'smooth' : 'instant' })
  }

  watch(toc, (element, previous) => {
    if (previous) resizeObserver?.unobserve(previous)
    if (element) resizeObserver?.observe(element)
    queueUpdate()
  })

  onMounted(() => {
    header = document.querySelector('.site-header')
    resizeObserver = new ResizeObserver(queueUpdate)
    if (header) resizeObserver.observe(header)
    if (toc.value) resizeObserver.observe(toc.value)
    mutationObserver = new MutationObserver(refreshHeadings)
    attachBody(body.value)
    window.addEventListener('scroll', queueUpdate, { passive: true })
    window.addEventListener('resize', queueUpdate)
  })

  function attachBody(element: HTMLElement | null, previous?: HTMLElement | null) {
    mutationObserver?.disconnect()
    if (previous) resizeObserver?.unobserve(previous)
    if (!element) {
      headings.value = []
      headingElements = []
      activeHeading.value = ''
      progress.value = 0
      return
    }
    refreshHeadings()
    resizeObserver?.observe(element)
    mutationObserver?.observe(element, { childList: true, subtree: true, characterData: true })
    let hashId = window.location.hash.slice(1)
    try { hashId = decodeURIComponent(hashId) } catch { /* Ignore a malformed URL fragment. */ }
    if (headings.value.some(heading => heading.id === hashId)) void goToHeading(hashId, false)
  }

  // API content may arrive after the component mounts.
  watch(body, (element, previous) => attachBody(element, previous), { flush: 'post' })

  onBeforeUnmount(() => {
    window.removeEventListener('scroll', queueUpdate)
    window.removeEventListener('resize', queueUpdate)
    resizeObserver?.disconnect()
    mutationObserver?.disconnect()
    if (frame) window.cancelAnimationFrame(frame)
  })

  return { headings, activeHeading, progress, tocExpanded, headerHeight, scrollOffset, goToHeading }
}
