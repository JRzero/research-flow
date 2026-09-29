package com.ruoyi.research.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class ResearchProgress extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long progressId;
    private Long projectId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date recordDate;
    private Integer progressPercent;
    private String completedWork;
    private String issues;
    private String nextPlan;
    private Long recorderUserId;
    private String recorderName;
    public Long getProgressId() { return progressId; }
    public void setProgressId(Long progressId) { this.progressId = progressId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Date getRecordDate() { return recordDate; }
    public void setRecordDate(Date recordDate) { this.recordDate = recordDate; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public String getCompletedWork() { return completedWork; }
    public void setCompletedWork(String completedWork) { this.completedWork = completedWork; }
    public String getIssues() { return issues; }
    public void setIssues(String issues) { this.issues = issues; }
    public String getNextPlan() { return nextPlan; }
    public void setNextPlan(String nextPlan) { this.nextPlan = nextPlan; }
    public Long getRecorderUserId() { return recorderUserId; }
    public void setRecorderUserId(Long recorderUserId) { this.recorderUserId = recorderUserId; }
    public String getRecorderName() { return recorderName; }
    public void setRecorderName(String recorderName) { this.recorderName = recorderName; }
}
