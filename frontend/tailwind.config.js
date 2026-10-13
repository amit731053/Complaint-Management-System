/** Same settings the pages used with the Tailwind CDN, now compiled once into static/tailwind.css */
module.exports = {
  darkMode: 'class',
  content: ['../src/main/resources/static/**/*.{html,js}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        mono: ['"Roboto Mono"', 'monospace']
      },
      colors: {
        brand: {
          light: '#f1f5fb', base: '#4f46e5', dark: '#312e81', surface: '#ffffff',
          textBase: '#334155', textMuted: '#64748b', border: '#e2e8f0'
        }
      }
    }
  }
};
