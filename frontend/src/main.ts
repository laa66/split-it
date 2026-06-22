import { createApp } from 'vue';
import { IonicVue } from '@ionic/vue';
import { createPinia } from 'pinia';

import App from './App.vue';
import router from './router';
import { i18n } from './i18n';

import '@ionic/vue/css/core.css';
import '@ionic/vue/css/normalize.css';
import '@ionic/vue/css/structure.css';
import '@ionic/vue/css/typography.css';

import './theme/variables.css';
import './theme/tailwind.css';

const app = createApp(App)
  .use(IonicVue)
  .use(createPinia())
  .use(router)
  .use(i18n);

router.isReady().then(() => app.mount('#app'));
