export const useBreakpoint = () => {
  const width = ref(0)
  const update = () => { width.value = window.innerWidth }

  onMounted(() => { update(); window.addEventListener('resize', update) })
  onUnmounted(() => { window.removeEventListener('resize', update) })

  return {
    width,
    isMobile:  computed(() => width.value < 768),
    isTablet:  computed(() => width.value >= 768 && width.value < 1024),
    isDesktop: computed(() => width.value >= 1024),
  }
}
