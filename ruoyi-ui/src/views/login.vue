<template>
  <main class="login">
    <section class="login-intro" aria-label="ResearchFlow 产品介绍">
      <div class="intro-brand">
        <img :src="brandMark" alt="" aria-hidden="true" />
        <div>
          <strong>ResearchFlow</strong>
          <span>科研项目全生命周期管理</span>
        </div>
      </div>

      <div class="intro-content">
        <span class="intro-eyebrow">RESEARCH OPERATIONS</span>
        <h1>让科研项目从申报到结项，始终可见、可追踪、可解释。</h1>
        <p>围绕项目工作空间组织审批、执行、经费、成果和验收，把科研管理从“填表”变成持续推进的工作过程。</p>

        <div class="intro-features">
          <div>
            <span class="feature-index">01</span>
            <div><strong>完整生命周期</strong><p>申报、立项、执行、验收形成一个连续闭环。</p></div>
          </div>
          <div>
            <span class="feature-index">02</span>
            <div><strong>风险可解释</strong><p>时间、进度、预算规则明确，AI 只做辅助解释。</p></div>
          </div>
          <div>
            <span class="feature-index">03</span>
            <div><strong>角色边界清晰</strong><p>负责人、科研管理员、管理者拥有不同工作视角。</p></div>
          </div>
        </div>
      </div>

      <div class="intro-foot">ResearchFlow · 科研项目管理 MVP</div>
    </section>

    <section class="login-panel" aria-labelledby="login-title">
      <div class="mobile-brand">
        <img :src="brandMark" alt="" aria-hidden="true" />
        <div><strong>ResearchFlow</strong><span>科研项目全生命周期管理</span></div>
      </div>

      <div class="login-card">
        <div class="login-heading">
          <span>欢迎回来</span>
          <h2 id="login-title">登录科研项目工作台</h2>
          <p>使用你的账号继续处理项目、审批与验收。</p>
        </div>

        <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top" class="login-form">
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              size="large"
              autocomplete="username"
              placeholder="请输入账号"
            >
              <template #prefix><svg-icon icon-class="user" class="input-icon" /></template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              size="large"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix><svg-icon icon-class="password" class="input-icon" /></template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="captchaEnabled" label="验证码" prop="code">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.code"
                size="large"
                autocomplete="off"
                placeholder="请输入验证码"
                @keyup.enter="handleLogin"
              >
                <template #prefix><svg-icon icon-class="validCode" class="input-icon" /></template>
              </el-input>
              <button type="button" class="captcha-button" aria-label="刷新验证码" @click="getCode">
                <img :src="codeUrl" alt="验证码，点击可刷新" />
              </button>
            </div>
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="loginForm.rememberMe">记住登录信息</el-checkbox>
            <span>演示环境</span>
          </div>

          <el-button
            :loading="loading"
            size="large"
            type="primary"
            class="login-submit"
            @click.prevent="handleLogin"
          >
            {{ loading ? '正在登录…' : '登录 ResearchFlow' }}
          </el-button>

          <div v-if="register" class="register-link">
            <router-link :to="'/register'">立即注册</router-link>
          </div>
        </el-form>

        <div class="demo-account">
          <div class="demo-title">
            <span>快速体验</span>
            <small>默认密码均为 admin123</small>
          </div>
          <div class="demo-roles">
            <button type="button" @click="useDemo('research_admin')"><strong>科研管理员</strong><span>research_admin</span></button>
            <button type="button" @click="useDemo('researcher')"><strong>项目负责人</strong><span>researcher</span></button>
            <button type="button" @click="useDemo('research_manager')"><strong>管理者</strong><span>research_manager</span></button>
          </div>
        </div>
      </div>

      <div class="login-footer">{{ footerContent }}</div>
    </section>
  </main>
</template>

<script setup>
import { getCodeImg } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'
import brandMark from '@/assets/logo/researchflow-mark.svg'

const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: 'research_admin',
  password: 'admin123',
  rememberMe: false,
  code: '',
  uuid: ''
})

const loginRules = {
  username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
  password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}

const codeUrl = ref('')
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function useDemo(username) {
  loginForm.value.username = username
  loginForm.value.password = 'admin123'
  proxy.$refs.loginRef?.clearValidate()
}

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (!valid) return

    loading.value = true
    if (loginForm.value.rememberMe) {
      Cookies.set('username', loginForm.value.username, { expires: 30 })
      Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 })
      Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 })
    } else {
      Cookies.remove('username')
      Cookies.remove('password')
      Cookies.remove('rememberMe')
    }

    userStore.login(loginForm.value).then(() => {
      const query = route.query
      const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
        if (cur !== 'redirect') acc[cur] = query[cur]
        return acc
      }, {})
      router.push({ path: redirect.value || '/', query: otherQueryParams })
    }).catch(() => {
      loading.value = false
      if (captchaEnabled.value) getCode()
    })
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
    code: '',
    uuid: ''
  }
}

getCode()
getCookie()
</script>

<style scoped lang="scss">
.login {
  min-height: 100dvh;
  display: grid;
  grid-template-columns: minmax(420px, 1.05fr) minmax(460px, .95fr);
  background: var(--rf-bg);
  color: var(--rf-text);
}

.login-intro {
  position: relative;
  min-height: 100dvh;
  padding: 48px clamp(36px, 5vw, 76px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  color: #f8fafc;
  background:
    radial-gradient(circle at 82% 14%, rgba(14, 165, 233, .18), transparent 30%),
    radial-gradient(circle at 8% 86%, rgba(37, 99, 235, .20), transparent 34%),
    #0b1220;
}
.login-intro::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: .17;
  background-image:
    linear-gradient(rgba(148, 163, 184, .14) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, .14) 1px, transparent 1px);
  background-size: 48px 48px;
}
.intro-brand, .intro-content, .intro-foot { position: relative; z-index: 1; }
.intro-brand { display: flex; align-items: center; gap: 13px; }
.intro-brand img { width: 44px; height: 44px; }
.intro-brand > div { display: flex; flex-direction: column; gap: 3px; }
.intro-brand strong { font-size: 17px; letter-spacing: .1px; }
.intro-brand span { color: #94a3b8; font-size: 12px; }

.intro-content { width: min(640px, 100%); margin: auto 0; padding: 70px 0; }
.intro-eyebrow { color: #7dd3fc; font-size: 12px; font-weight: 750; letter-spacing: 1.8px; }
.intro-content h1 { margin: 16px 0 18px; max-width: 620px; font-size: clamp(30px, 3.3vw, 48px); line-height: 1.18; letter-spacing: -1.5px; text-wrap: balance; }
.intro-content > p { max-width: 580px; margin: 0; color: #b8c5d6; font-size: 15px; line-height: 1.8; }

.intro-features { margin-top: 38px; display: grid; gap: 14px; }
.intro-features > div {
  max-width: 560px;
  padding: 13px 0;
  border-top: 1px solid rgba(148, 163, 184, .18);
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 12px;
}
.feature-index { padding-top: 2px; color: #60a5fa; font-size: 12px; font-variant-numeric: tabular-nums; }
.intro-features strong { font-size: 14px; }
.intro-features p { margin: 4px 0 0; color: #94a3b8; font-size: 12px; line-height: 1.55; }
.intro-foot { margin-top: auto; color: #64748b; font-size: 12px; }

.login-panel {
  min-height: 100dvh;
  padding: 40px clamp(28px, 5vw, 72px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: var(--rf-surface);
}
.mobile-brand { display: none; }
.login-card { width: min(440px, 100%); margin: auto; }
.login-heading > span { color: var(--rf-primary); font-size: 12px; font-weight: 700; }
.login-heading h2 { margin: 8px 0 7px; color: var(--rf-text); font-size: 26px; line-height: 1.3; letter-spacing: -.4px; }
.login-heading p { margin: 0 0 26px; color: var(--rf-text-muted); font-size: 13px; line-height: 1.6; }

.login-form :deep(.el-form-item) { margin-bottom: 18px; }
.login-form :deep(.el-form-item__label) { padding-bottom: 7px; line-height: 1.2; }
.login-form :deep(.el-input__wrapper) { min-height: 46px; box-shadow: 0 0 0 1px var(--rf-border) inset; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--rf-primary) inset, var(--rf-focus); }
.input-icon { width: 16px; height: 16px; color: var(--rf-text-muted); }

.captcha-row { width: 100%; display: grid; grid-template-columns: minmax(0, 1fr) 128px; gap: 10px; }
.captcha-button {
  min-height: 46px;
  padding: 0;
  border: 1px solid var(--rf-border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--rf-surface-subtle);
  cursor: pointer;
}
.captcha-button img { width: 100%; height: 44px; display: block; object-fit: cover; }

.login-options { min-height: 40px; margin-top: -4px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.login-options > span { color: var(--rf-text-muted); font-size: 12px; }
.login-submit { width: 100%; min-height: 46px; margin-top: 7px; font-weight: 650; }
.register-link { margin-top: 14px; text-align: center; color: var(--rf-primary); font-size: 13px; }

.demo-account { margin-top: 28px; padding-top: 20px; border-top: 1px solid var(--rf-border); }
.demo-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.demo-title span { color: var(--rf-text-secondary); font-size: 12px; font-weight: 700; }
.demo-title small { color: var(--rf-text-muted); font-size: 11px; }
.demo-roles { margin-top: 10px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.demo-roles button {
  min-height: 58px;
  padding: 9px 10px;
  border: 1px solid var(--rf-border);
  border-radius: 10px;
  background: var(--rf-surface-subtle);
  color: var(--rf-text-secondary);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--rf-motion-fast) ease, background var(--rf-motion-fast) ease;
}
.demo-roles button:hover { border-color: var(--rf-primary-border); background: var(--rf-primary-soft); }
.demo-roles strong, .demo-roles span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.demo-roles strong { font-size: 12px; }
.demo-roles span { margin-top: 3px; color: var(--rf-text-muted); font-size: 10px; }

.login-footer { width: min(440px, 100%); margin: 24px auto 0; color: var(--rf-text-muted); font-size: 11px; text-align: center; }

@media (max-width: 960px) {
  .login { grid-template-columns: 1fr; }
  .login-intro { display: none; }
  .login-panel { padding: 28px 20px; }
  .mobile-brand {
    width: min(440px, 100%);
    margin: 0 auto 42px;
    display: flex;
    align-items: center;
    gap: 11px;
  }
  .mobile-brand img { width: 42px; height: 42px; }
  .mobile-brand > div { display: flex; flex-direction: column; gap: 2px; }
  .mobile-brand strong { color: var(--rf-text); font-size: 16px; }
  .mobile-brand span { color: var(--rf-text-muted); font-size: 11px; }
}

@media (max-width: 520px) {
  .login-panel { justify-content: flex-start; padding-top: 24px; }
  .mobile-brand { margin-bottom: 34px; }
  .login-heading h2 { font-size: 23px; }
  .captcha-row { grid-template-columns: 1fr 112px; }
  .demo-roles { grid-template-columns: 1fr; }
  .demo-roles button { min-height: 52px; }
  .demo-title { align-items: flex-start; flex-direction: column; gap: 4px; }
}
</style>
