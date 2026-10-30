import { Chart, registerables } from 'chart.js'
Chart.register(...registerables)

export default defineNuxtPlugin(() => {})