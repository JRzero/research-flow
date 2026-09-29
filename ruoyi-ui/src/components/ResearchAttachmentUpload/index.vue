<template>
  <div class="research-attachment-upload">
    <el-upload
      v-if="!disabled"
      ref="uploadRef"
      multiple
      :action="uploadUrl"
      :headers="headers"
      :file-list="uploadFileList"
      :limit="limit"
      :before-upload="beforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-exceed="handleExceed"
      :show-file-list="false"
    >
      <el-button type="primary" plain>
        <el-icon aria-hidden="true"><UploadFilled /></el-icon>
        添加附件
      </el-button>
    </el-upload>

    <div v-if="!disabled" class="upload-tip">
      最多 {{ limit }} 个文件，单个不超过 {{ fileSize }}MB。支持 Word、Excel、PPT、PDF、图片和 ZIP。
    </div>

    <ul v-if="attachments.length" class="attachment-list" aria-label="项目申报附件">
      <li v-for="(file, index) in attachments" :key="file.url + '-' + index" class="attachment-item">
        <a
          class="attachment-link"
          :href="baseUrl + file.url"
          target="_blank"
          rel="noopener noreferrer"
          :title="file.name"
        >
          <span class="file-icon" aria-hidden="true"><el-icon><Document /></el-icon></span>
          <span class="file-copy">
            <strong>{{ file.name }}</strong>
            <small>{{ extensionLabel(file.name) }}</small>
          </span>
          <el-icon class="open-icon" aria-hidden="true"><TopRight /></el-icon>
        </a>
        <el-button
          v-if="!disabled"
          text
          type="danger"
          aria-label="删除附件"
          @click="removeAttachment(index)"
        >
          删除
        </el-button>
      </li>
    </ul>

    <div v-else class="empty-attachments">
      <el-icon aria-hidden="true"><Paperclip /></el-icon>
      <span>{{ disabled ? '未上传申报附件' : '暂未上传附件，可添加任务书、预算说明或论证材料。' }}</span>
    </div>
  </div>
</template>

<script setup>
import { getToken } from '@/utils/auth'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  limit: {
    type: Number,
    default: 10
  },
  fileSize: {
    type: Number,
    default: 10
  }
})

const emit = defineEmits(['update:modelValue'])
const { proxy } = getCurrentInstance()
const baseUrl = import.meta.env.VITE_APP_BASE_API
const uploadUrl = import.meta.env.VITE_APP_BASE_API + '/common/upload'
const headers = { Authorization: 'Bearer ' + getToken() }

const attachments = ref([])
const uploadFileList = ref([])
const allowedTypes = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'png', 'jpg', 'jpeg', 'zip']

watch(
  () => props.modelValue,
  (value) => {
    attachments.value = parseAttachments(value)
    uploadFileList.value = attachments.value.map((file, index) => ({
      name: file.name,
      url: file.url,
      uid: file.uid || String(Date.now()) + '-' + index
    }))
  },
  { immediate: true }
)

function parseAttachments(value) {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed)
      ? parsed.filter(file => file && file.url).map(file => ({
          name: file.name || file.originalFilename || file.url.split('/').pop(),
          url: file.url
        }))
      : []
  } catch {
    return value.split(',').filter(Boolean).map(url => ({
      name: url.split('/').pop(),
      url
    }))
  }
}

function emitValue() {
  emit('update:modelValue', JSON.stringify(attachments.value))
}

function beforeUpload(file) {
  const ext = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : ''
  if (!allowedTypes.includes(ext)) {
    proxy.$modal.msgError('文件格式不支持，请上传 Word、Excel、PPT、PDF、图片或 ZIP 文件')
    return false
  }
  if (file.name.includes(',')) {
    proxy.$modal.msgError('文件名不能包含英文逗号')
    return false
  }
  if (file.size / 1024 / 1024 > props.fileSize) {
    proxy.$modal.msgError('单个附件不能超过 ' + props.fileSize + 'MB')
    return false
  }
  return true
}

function handleSuccess(res, file) {
  if (res.code !== 200) {
    proxy.$modal.msgError(res.msg || '附件上传失败')
    return
  }
  attachments.value.push({
    name: res.originalFilename || file.name,
    url: res.fileName
  })
  emitValue()
  proxy.$modal.msgSuccess('附件上传成功')
}

function handleError() {
  proxy.$modal.msgError('附件上传失败，请稍后重试')
}

function handleExceed() {
  proxy.$modal.msgError('最多上传 ' + props.limit + ' 个附件')
}

function removeAttachment(index) {
  attachments.value.splice(index, 1)
  uploadFileList.value.splice(index, 1)
  emitValue()
}

function extensionLabel(name = '') {
  const ext = name.includes('.') ? name.split('.').pop().toUpperCase() : '文件'
  return ext + ' 文件'
}
</script>

<style scoped lang="scss">
.research-attachment-upload { width: 100%; }
.upload-tip { margin-top: 8px; color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }
.attachment-list { margin: 12px 0 0; padding: 0; display: flex; flex-direction: column; gap: 8px; list-style: none; }
.attachment-item {
  min-height: 52px;
  padding: 7px 8px 7px 10px;
  border: 1px solid var(--rf-border);
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--rf-surface);
}
.attachment-link {
  min-width: 0;
  min-height: 44px;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  color: inherit;
  text-decoration: none;
  border-radius: 8px;
}
.attachment-link:hover strong { color: var(--rf-primary); }
.file-icon { width: 36px; height: 36px; flex: 0 0 36px; border-radius: 9px; display: grid; place-items: center; background: var(--rf-primary-soft); color: var(--rf-primary); font-size: 18px; }
.file-copy { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.file-copy strong { overflow: hidden; color: var(--rf-text-secondary); font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.file-copy small { color: var(--rf-text-muted); font-size: 11px; }
.open-icon { flex: 0 0 auto; color: var(--rf-text-muted); }
.empty-attachments { min-height: 56px; margin-top: 8px; padding: 12px 14px; border: 1px dashed var(--rf-border-strong); border-radius: 10px; display: flex; align-items: center; gap: 9px; background: var(--rf-surface-subtle); color: var(--rf-text-muted); font-size: 12px; line-height: 1.5; }
.empty-attachments .el-icon { flex: 0 0 auto; font-size: 18px; }
</style>
