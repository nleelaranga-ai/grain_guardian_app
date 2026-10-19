/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}"
  ],
  theme: {
    extend: {
      colors: {
        grain: {
          green: "#1B7F3A",
          darkGreen: "#0E5A27",
          lightGreen: "#EAF8EF",
          yellow: "#F59E0B",
          red: "#DC2626",
          bg: "#F8FBF7",
          surface: "#FFFFFF",
          textPrimary: "#1E293B",
          textSecondary: "#64748B"
        }
      }
    }
  },
  plugins: []
};
