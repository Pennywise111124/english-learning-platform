// ========================================
// LinguistAI - Shared Tailwind Configuration
// Design tokens extracted from Stitch design system
// ========================================

tailwind.config = {
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        // Primary colors
        "primary": "#004ac6",
        "primary-container": "#2563eb",
        "primary-fixed": "#dbe1ff",
        "primary-fixed-dim": "#b4c5ff",
        "on-primary": "#ffffff",
        "on-primary-container": "#eeefff",
        "on-primary-fixed": "#00174b",
        "on-primary-fixed-variant": "#003ea8",
        "inverse-primary": "#b4c5ff",

        // Secondary colors
        "secondary": "#006a61",
        "secondary-container": "#86f2e4",
        "secondary-fixed": "#89f5e7",
        "secondary-fixed-dim": "#6bd8cb",
        "on-secondary": "#ffffff",
        "on-secondary-container": "#006f66",
        "on-secondary-fixed": "#00201d",
        "on-secondary-fixed-variant": "#005049",

        // Tertiary colors
        "tertiary": "#ad0033",
        "tertiary-container": "#d22348",
        "tertiary-fixed": "#ffdadb",
        "tertiary-fixed-dim": "#ffb2b7",
        "on-tertiary": "#ffffff",
        "on-tertiary-container": "#ffecec",
        "on-tertiary-fixed": "#40000d",
        "on-tertiary-fixed-variant": "#92002a",

        // Error colors
        "error": "#ba1a1a",
        "error-container": "#ffdad6",
        "on-error": "#ffffff",
        "on-error-container": "#93000a",

        // Surface colors
        "surface": "#f8f9ff",
        "surface-dim": "#cbdbf5",
        "surface-bright": "#f8f9ff",
        "surface-container-lowest": "#ffffff",
        "surface-container-low": "#eff4ff",
        "surface-container": "#e5eeff",
        "surface-container-high": "#dce9ff",
        "surface-container-highest": "#d3e4fe",
        "surface-variant": "#d3e4fe",
        "surface-tint": "#0053db",
        "on-surface": "#0b1c30",
        "on-surface-variant": "#434655",

        // Background
        "background": "#f8f9ff",
        "on-background": "#0b1c30",

        // Outline
        "outline": "#737686",
        "outline-variant": "#c3c6d7",

        // Inverse
        "inverse-surface": "#213145",
        "inverse-on-surface": "#eaf1ff"
      },

      borderRadius: {
        "DEFAULT": "0.25rem",
        "lg": "0.5rem",
        "xl": "0.75rem",
        "full": "9999px"
      },

      spacing: {
        "unit": "8px",
        "stack-sm": "12px",
        "stack-md": "24px",
        "stack-lg": "48px",
        "gutter": "24px",
        "margin-mobile": "16px",
        "margin-desktop": "40px",
        "container-max": "1280px"
      },

      fontFamily: {
        "display-lg": ["Plus Jakarta Sans", "sans-serif"],
        "headline-lg": ["Plus Jakarta Sans", "sans-serif"],
        "headline-md": ["Plus Jakarta Sans", "sans-serif"],
        "body-lg": ["Inter", "sans-serif"],
        "body-md": ["Inter", "sans-serif"],
        "label-md": ["Inter", "sans-serif"],
        "label-sm": ["Inter", "sans-serif"]
      },

      fontSize: {
        "display-lg": ["48px", { lineHeight: "1.2", letterSpacing: "-0.02em", fontWeight: "700" }],
        "headline-lg": ["32px", { lineHeight: "1.3", fontWeight: "600" }],
        "headline-md": ["24px", { lineHeight: "1.4", fontWeight: "600" }],
        "body-lg": ["18px", { lineHeight: "1.6", fontWeight: "400" }],
        "body-md": ["16px", { lineHeight: "1.5", fontWeight: "400" }],
        "label-md": ["14px", { lineHeight: "1", letterSpacing: "0.01em", fontWeight: "500" }],
        "label-sm": ["12px", { lineHeight: "1", fontWeight: "600" }]
      }
    }
  }
};
