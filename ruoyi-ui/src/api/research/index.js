import request from '@/utils/request'

export function getResearchDashboard() {
  return request({ url: '/research/dashboard', method: 'get' })
}

export function listResearchProjects(params) {
  return request({ url: '/research/projects', method: 'get', params })
}

export function getResearchProject(projectId) {
  return request({ url: `/research/projects/${projectId}`, method: 'get' })
}

export function createResearchProject(data) {
  return request({ url: '/research/projects', method: 'post', data })
}

export function updateResearchProject(projectId, data) {
  return request({ url: `/research/projects/${projectId}`, method: 'put', data })
}

export function submitResearchProject(projectId) {
  return request({ url: `/research/projects/${projectId}/submit`, method: 'post' })
}

export function approveResearchProject(projectId, comment) {
  return request({ url: `/research/projects/${projectId}/approve`, method: 'post', data: { comment } })
}

export function rejectResearchProject(projectId, comment) {
  return request({ url: `/research/projects/${projectId}/reject`, method: 'post', data: { comment } })
}

export function startResearchProject(projectId) {
  return request({ url: `/research/projects/${projectId}/start`, method: 'post' })
}

export function addResearchMilestone(projectId, data) {
  return request({ url: `/research/projects/${projectId}/milestones`, method: 'post', data })
}

export function completeResearchMilestone(projectId, milestoneId) {
  return request({ url: `/research/projects/${projectId}/milestones/${milestoneId}/complete`, method: 'post' })
}

export function addResearchProgress(projectId, data) {
  return request({ url: `/research/projects/${projectId}/progress`, method: 'post', data })
}

export function addResearchExpense(projectId, data) {
  return request({ url: `/research/projects/${projectId}/expenses`, method: 'post', data })
}

export function addResearchDeliverable(projectId, data) {
  return request({ url: `/research/projects/${projectId}/deliverables`, method: 'post', data })
}

export function submitResearchAcceptance(projectId, data) {
  return request({ url: `/research/projects/${projectId}/acceptance`, method: 'post', data })
}

export function approveResearchAcceptance(projectId, comment) {
  return request({ url: `/research/projects/${projectId}/acceptance/approve`, method: 'post', data: { comment } })
}

export function rejectResearchAcceptance(projectId, comment) {
  return request({ url: `/research/projects/${projectId}/acceptance/reject`, method: 'post', data: { comment } })
}

export function listResearchRisks() {
  return request({ url: '/research/risks', method: 'get' })
}

export function generateResearchProposal(description) {
  return request({ url: '/research/ai/proposal', method: 'post', data: { description } })
}
