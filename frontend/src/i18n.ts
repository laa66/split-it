import { createI18n } from 'vue-i18n';
import pl from './locales/pl.json';
import en from './locales/en.json';

const STORAGE_KEY = 'splitit-locale';

export type AppLocale = 'pl' | 'en';

function initialLocale(): AppLocale {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored === 'en' ? 'en' : 'pl';
}

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale(),
  fallbackLocale: 'pl',
  messages: { pl, en },
});

// Runtime language switch — persisted across sessions.
export function setLocale(locale: AppLocale): void {
  i18n.global.locale.value = locale;
  localStorage.setItem(STORAGE_KEY, locale);
}
