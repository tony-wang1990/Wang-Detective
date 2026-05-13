import { ref } from 'vue';

export type ThemeName = 'light' | 'dark';

const theme = ref<ThemeName>((localStorage.getItem('theme') as ThemeName) || 'light');

export function applyTheme(nextTheme: ThemeName) {
  theme.value = nextTheme;
  document.documentElement.classList.toggle('dark', nextTheme === 'dark');
  localStorage.setItem('theme', nextTheme);
}

export function toggleTheme() {
  applyTheme(theme.value === 'dark' ? 'light' : 'dark');
}

export function useTheme() {
  applyTheme(theme.value === 'dark' ? 'dark' : 'light');
  return {
    theme,
    toggleTheme
  };
}
