<template>
  <div class="document-upload">
    <el-upload
      multiple
      :action="uploadUrl"
      :headers="headers"
      :show-file-list="false"
      :limit="limit"
      :before-upload="beforeUpload"
      :on-success="success"
      :on-error="error"
      :on-exceed="exceed"
    >
      <el-button plain><el-icon><UploadFilled /></el-icon> 添加文件</el-button>
    </el-upload>
    <div class="hint">最多 {{ limit }} 个，单文件 ≤ {{ fileSize }}MB；支持 Word / Excel / PPT / PDF / 图片 / ZIP。</div>
    <div v-if="modelValue?.length" class="docs">
      <div v-for="(doc,index) in modelValue" :key="doc.storageKey || index" class="doc">
        <el-icon><Document /></el-icon>
        <span :title="doc.fileName">{{ doc.fileName }}</span>
        <el-button text type="danger" @click="remove(index)">删除</el-button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { getToken } from '@/utils/auth'
const props=defineProps({modelValue:{type:Array,default:()=>[]},limit:{type:Number,default:10},fileSize:{type:Number,default:10}})
const emit=defineEmits(['update:modelValue'])
const {proxy}=getCurrentInstance()
const uploadUrl=import.meta.env.VITE_APP_BASE_API+'/common/upload'
const headers={Authorization:'Bearer '+getToken()}
const allowed=['doc','docx','xls','xlsx','ppt','pptx','pdf','png','jpg','jpeg','zip']
function beforeUpload(file){const ext=file.name.split('.').pop().toLowerCase();if(!allowed.includes(ext)){proxy.$modal.msgError('不支持该文件格式');return false}if(file.size/1024/1024>props.fileSize){proxy.$modal.msgError('单文件不能超过 '+props.fileSize+'MB');return false}return true}
function success(res,file){if(res.code!==200)return proxy.$modal.msgError(res.msg||'上传失败');emit('update:modelValue',[...(props.modelValue||[]),{fileName:res.originalFilename||file.name,storageKey:res.fileName,mimeType:file.raw?.type||'',fileSize:file.raw?.size||0}])}
function error(){proxy.$modal.msgError('上传失败')}
function exceed(){proxy.$modal.msgError('最多上传 '+props.limit+' 个文件')}
function remove(i){const next=[...(props.modelValue||[])];next.splice(i,1);emit('update:modelValue',next)}
</script>
<style scoped lang="scss">
.document-upload{width:100%}.hint{margin-top:6px;color:var(--rf-text-muted);font-size:11px}.docs{margin-top:8px;display:grid;gap:6px}.doc{min-height:36px;padding:5px 8px;border:1px solid var(--rf-border);border-radius:8px;display:flex;align-items:center;gap:8px;background:var(--rf-surface-subtle)}.doc>span{min-width:0;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:12px;color:var(--rf-text-secondary)}
</style>
