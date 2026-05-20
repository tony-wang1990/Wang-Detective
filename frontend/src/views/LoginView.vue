<script setup lang="ts">
import { onMounted, onBeforeUnmount, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { apiPost, type LoginResponse } from '../api/http';
import { useTheme } from '../composables/useTheme';

const router = useRouter();
const { theme, toggleTheme } = useTheme();
const loading = ref(false);
const error = ref('');
const mfaRequired = ref(false);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const form = reactive({
  username: 'admin',
  password: '',
  mfaCode: ''
});

let animFrame: number | null = null;

// ── Particle Animation ──────────────────────────────────────
function initParticles(canvas: HTMLCanvasElement) {
  const ctx = canvas.getContext('2d')!;
  let width = canvas.width = window.innerWidth;
  let height = canvas.height = window.innerHeight;

  const COLORS = ['#38bdf8', '#818cf8', '#2dd4bf', '#60a5fa'];

  const particles = Array.from({ length: 72 }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.4,
    vy: (Math.random() - 0.5) * 0.4,
    r: Math.random() * 2.2 + 0.4,
    color: COLORS[Math.floor(Math.random() * COLORS.length)],
    alpha: Math.random() * 0.6 + 0.15,
  }));

  function draw() {
    ctx.clearRect(0, 0, width, height);

    // Draw connections
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x;
        const dy = particles[i].y - particles[j].y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 130) {
          ctx.beginPath();
          ctx.strokeStyle = `rgba(56, 189, 248, ${0.12 * (1 - dist / 130)})`;
          ctx.lineWidth = 0.6;
          ctx.moveTo(particles[i].x, particles[i].y);
          ctx.lineTo(particles[j].x, particles[j].y);
          ctx.stroke();
        }
      }
    }

    // Draw particles
    for (const p of particles) {
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fillStyle = p.color + Math.round(p.alpha * 255).toString(16).padStart(2, '0');
      ctx.fill();

      p.x += p.vx;
      p.y += p.vy;

      if (p.x < -10) p.x = width + 10;
      if (p.x > width + 10) p.x = -10;
      if (p.y < -10) p.y = height + 10;
      if (p.y > height + 10) p.y = -10;
    }

    animFrame = requestAnimationFrame(draw);
  }

  function onResize() {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
  }

  window.addEventListener('resize', onResize);
  draw();

  return () => {
    window.removeEventListener('resize', onResize);
    if (animFrame !== null) cancelAnimationFrame(animFrame);
  };
}

let cleanupParticles: (() => void) | null = null;

async function loadMfaState() {
  try {
    const result = await apiPost<boolean>('/sys/getEnableMfa', {});
    mfaRequired.value = Boolean(result.data);
  } catch {
    mfaRequired.value = false;
  }
}

async function submit() {
  loading.value = true;
  error.value = '';
  try {
    const result = await apiPost<LoginResponse>('/sys/login', {
      account: form.username,
      password: form.password,
      mfaCode: mfaRequired.value ? form.mfaCode : undefined
    });
    if (!result.success || !result.data?.token) {
      throw new Error(result.msg || '登录失败');
    }
    sessionStorage.setItem('token', result.data.token);
    if (result.data.currentVersion) {
      localStorage.setItem('currentVersion', result.data.currentVersion);
    }
    if (result.data.latestVersion) {
      localStorage.setItem('latestVersion', result.data.latestVersion);
    }
    await router.replace('/dashboard/home');
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadMfaState();
  if (canvasRef.value) {
    cleanupParticles = initParticles(canvasRef.value);
  }
});

onBeforeUnmount(() => {
  cleanupParticles?.();
});
</script>

<template>
  <main class="wd-login">
    <!-- Particle Canvas -->
    <canvas ref="canvasRef" class="wd-login-canvas" />

    <!-- Brand Panel (left) -->
    <section class="wd-login-brand">
      <div class="wd-login-mark">
        <div class="wd-login-logo xl">W</div>
        <div>
          <span>WANG DETECTIVE</span>
          <h1>W-探长</h1>
          <p>OCI Operations Console</p>
        </div>
      </div>

      <div class="wd-login-signal">
        <strong>OCI</strong>
        <span>Compute · Network · Logs · Ops</span>
      </div>

      <ul>
        <li>资源、任务、日志集中管理</li>
        <li>系统诊断与运维入口统一呈现</li>
        <li>AI 对话 · 救援中心 · 实时监控</li>
        <li>深色玻璃主题，极致视觉体验</li>
      </ul>
    </section>

    <!-- Login Panel (right) -->
    <section class="wd-login-panel">
      <div class="wd-login-card">
        <div class="wd-login-card-title">
          <div class="wd-logo">W</div>
          <div>
            <h2>登录控制台</h2>
            <span>OCI 资源与运维管理</span>
          </div>
        </div>

        <form @submit.prevent="submit">
          <label>
            <span>账号</span>
            <input v-model="form.username" autocomplete="username" placeholder="输入用户名" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="form.password" type="password" autocomplete="current-password" placeholder="输入密码" />
          </label>
          <label v-if="mfaRequired">
            <span>MFA 验证码</span>
            <input v-model="form.mfaCode" inputmode="numeric" autocomplete="one-time-code" placeholder="6 位动态验证码" />
          </label>
          <p v-if="error" class="wd-error">{{ error }}</p>
          <button type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录控制台' }}
          </button>
        </form>
      </div>

      <footer>
        <span>© 2026 Tony Wang</span>
        <button type="button" @click="toggleTheme">{{ theme === 'dark' ? '☀️ 开灯' : '🌙 关灯' }}</button>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.wd-login-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.wd-login-brand,
.wd-login-panel {
  position: relative;
  z-index: 1;
}
</style>
