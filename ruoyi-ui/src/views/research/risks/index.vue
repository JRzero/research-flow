<template>
  <div v-loading="loading" class="risk-page">
    <section class="risk-explain" aria-labelledby="risk-rule-title">
      <div class="rule-icon" aria-hidden="true"><el-icon><Warning /></el-icon></div>
      <div class="rule-copy">
        <span class="eyebrow">RULE-BASED RISK</span>
        <strong id="risk-rule-title">风险判断基于确定性规则</strong>
        <p>当前 MVP 比较时间进度、任务完成度和预算执行率。AI 可以解释风险，但不决定项目业务状态。</p>
      </div>
      <div class="risk-total">
        <strong class="rf-tabular">{{ risks.length }}</strong>
        <span>个项目需关注</span>
      </div>
    </section>

    <div v-if="risks.length" class="risk-grid">
      <button
        v-for="p in risks"
        :key="p.projectId"
        type="button"
        class="risk-card"
        :class="p.riskLevel?.toLowerCase()"
        @click="openProject(p.projectId)"
      >
        <div class="risk-card-head">
          <span :class="['risk-tag', p.riskLevel?.toLowerCase()]">
            <el-icon aria-hidden="true"><WarningFilled /></el-icon>
            {{ p.riskLevel === 'HIGH' ? '高风险' : '需关注' }}
          </span>
          <span class="project-no">{{ p.projectNo }}</span>
        </div>

        <h2>{{ p.projectName }}</h2>
        <div class="risk-reason">
          <span>风险证据</span>
          <p>{{ p.riskReason }}</p>
        </div>

        <div class="risk-metrics">
          <div>
            <span>项目进度</span>
            <strong class="rf-tabular">{{ p.progress || 0 }}%</strong>
          </div>
          <div>
            <span>预算执行</span>
            <strong class="rf-tabular">{{ budgetRate(p) }}%</strong>
          </div>
          <div>
            <span>计划结束</span>
            <strong class="rf-tabular">{{ p.plannedEndDate }}</strong>
          </div>
        </div>

        <div class="risk-footer">
          <span>进入项目查看完整上下文</span>
          <el-icon aria-hidden="true"><ArrowRight /></el-icon>
        </div>
      </button>
    </div>

    <div v-else class="healthy-empty">
      <div class="healthy-mark" aria-hidden="true"><el-icon><CircleCheckFilled /></el-icon></div>
      <h2>当前没有需要关注的风险项目</h2>
      <p>项目时间、任务进度和预算执行暂未触发风险规则。</p>
      <el-button type="primary" plain @click="router.push('/research/projects')">查看全部项目</el-button>
    </div>
  </div>
</template>

<script setup>
import { listResearchRisks } from '@/api/research'

const router = useRouter()
const loading = ref(false)
const risks = ref([])

async function load() {
  loading.value = true
  try {
    const res = await listResearchRisks()
    risks.value = res.data || res || []
  } finally {
    loading.value = false
  }
}

function budgetRate(p) {
  const total = Number(p.totalBudget || 0)
  return total ? Math.round(Number(p.usedBudget || 0) * 100 / total) : 0
}

function openProject(id) {
  router.push('/research/projects/' + id)
}

onMounted(load)
</script>

<style scoped lang="scss">
.risk-page { display: flex; flex-direction: column; gap: 16px; }

.risk-explain {
  min-height: 92px;
  padding: 17px 18px;
  border: 1px solid color-mix(in srgb, var(--rf-warning) 22%, var(--rf-border));
  border-radius: var(--rf-radius-lg);
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  background: var(--rf-warning-soft);
}
.rule-icon { width: 44px; height: 44px; border-radius: 12px; display: grid; place-items: center; background: var(--rf-surface); color: var(--rf-warning); font-size: 21px; box-shadow: var(--rf-shadow-sm); }
.rule-copy { min-width: 0; }
.eyebrow { display: block; margin-bottom: 3px; color: var(--rf-warning); font-size: 10px; font-weight: 750; letter-spacing: 1.2px; }
.rule-copy strong { color: var(--rf-text); font-size: 14px; }
.rule-copy p { max-width: 820px; margin: 5px 0 0; color: var(--rf-text-secondary); font-size: 12px; line-height: 1.55; }
.risk-total { min-width: 118px; padding-left: 18px; border-left: 1px solid color-mix(in srgb, var(--rf-warning) 24%, transparent); display: flex; flex-direction: column; align-items: flex-end; }
.risk-total strong { color: var(--rf-warning); font-size: 28px; }
.risk-total span { margin-top: 3px; color: var(--rf-text-muted); font-size: 11px; }

.risk-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.risk-card {
  min-width: 0;
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--rf-warning) 26%, var(--rf-border));
  border-radius: var(--rf-radius-lg);
  display: block;
  background: var(--rf-surface);
  color: inherit;
  text-align: left;
  cursor: pointer;
  box-shadow: var(--rf-shadow-sm);
  transition: transform var(--rf-motion-base) ease, border-color var(--rf-motion-base) ease, box-shadow var(--rf-motion-base) ease;
}
.risk-card.high { border-color: color-mix(in srgb, var(--rf-danger) 28%, var(--rf-border)); }
.risk-card:hover { transform: translateY(-2px); border-color: var(--rf-warning); box-shadow: var(--rf-shadow-md); }
.risk-card.high:hover { border-color: var(--rf-danger); }
.risk-card:focus-visible { box-shadow: var(--rf-focus), var(--rf-shadow-md); }

.risk-card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.risk-tag {
  min-height: 27px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: var(--rf-warning-soft);
  color: var(--rf-warning);
  font-size: 12px;
  font-weight: 700;
}
.risk-tag.high { background: var(--rf-danger-soft); color: var(--rf-danger); }
.project-no { color: var(--rf-text-muted); font-size: 12px; }

.risk-card h2 { margin: 14px 0 12px; color: var(--rf-text); font-size: 16px; font-weight: 700; line-height: 1.45; overflow-wrap: anywhere; }
.risk-reason { min-height: 72px; padding: 12px 13px; border-radius: 10px; background: var(--rf-surface-subtle); }
.risk-reason > span { color: var(--rf-text-muted); font-size: 11px; font-weight: 650; }
.risk-reason p { margin: 5px 0 0; color: var(--rf-text-secondary); font-size: 12px; line-height: 1.6; }

.risk-metrics { margin-top: 14px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.risk-metrics div { min-width: 0; padding: 10px; border: 1px solid var(--rf-border); border-radius: 9px; display: flex; flex-direction: column; gap: 5px; background: var(--rf-surface); }
.risk-metrics span { color: var(--rf-text-muted); font-size: 11px; }
.risk-metrics strong { overflow-wrap: anywhere; color: var(--rf-text-secondary); font-size: 12px; }

.risk-footer { margin-top: 14px; padding-top: 13px; border-top: 1px solid var(--rf-border); display: flex; align-items: center; justify-content: space-between; gap: 10px; color: var(--rf-primary); font-size: 12px; font-weight: 650; }

.healthy-empty {
  min-height: 360px;
  padding: 48px 20px;
  border: 1px solid var(--rf-border);
  border-radius: var(--rf-radius-lg);
  display: grid;
  place-items: center;
  align-content: center;
  background: var(--rf-surface);
  text-align: center;
}
.healthy-mark { width: 58px; height: 58px; border-radius: 16px; display: grid; place-items: center; background: var(--rf-success-soft); color: var(--rf-success); font-size: 30px; }
.healthy-empty h2 { margin: 15px 0 6px; color: var(--rf-text); font-size: 17px; }
.healthy-empty p { max-width: 480px; margin: 0 0 18px; color: var(--rf-text-muted); font-size: 12px; line-height: 1.6; }

@media (max-width: 850px) {
  .risk-grid { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .risk-explain { grid-template-columns: 44px 1fr; }
  .risk-total { grid-column: 1 / -1; padding: 12px 0 0; border-top: 1px solid color-mix(in srgb, var(--rf-warning) 24%, transparent); border-left: 0; align-items: flex-start; flex-direction: row; gap: 7px; }
  .risk-total strong { font-size: 20px; }
  .risk-total span { margin-top: 4px; }
  .risk-metrics { grid-template-columns: 1fr 1fr; }
  .risk-metrics div:last-child { grid-column: 1 / -1; }
}
</style>
