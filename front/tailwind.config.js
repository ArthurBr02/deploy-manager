/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      colors: {
        accent: {
          DEFAULT: '#4f46e5',
          hover: '#4338ca',
          subtle: '#eef0ff',
          text: '#3730a3',
        },
        warm: {
          bg: '#fafaf8',
          muted: '#f4f3ef',
          panel: '#ffffff',
          subtle: '#f7f6f2',
          sidebar: '#faf9f6',
          border: '#e7e5df',
          'border-strong': '#d6d3cb',
        },
        status: {
          success: '#16a34a',
          'success-bg': '#e8f5ed',
          failure: '#dc2626',
          'failure-bg': '#fbeaea',
          progress: '#d97706',
          'progress-bg': '#fdf3e4',
          pending: '#6b6a64',
          'pending-bg': '#f0efea',
          cancelled: '#64748b',
          'cancelled-bg': '#eef1f4',
        },
      },
    },
  },
  plugins: [],
}
