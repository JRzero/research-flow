package com.ruoyi.research.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class ResearchAcceptance extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long acceptanceId;
    private Long projectId;
    private String projectSummary;
    private String completionStatement;
    private String unfinishedItems;
    private String acceptanceNote;
    private Long reviewerId;
    private String reviewerName;
    private String reviewComment;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submittedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewedAt;
    public Long getAcceptanceId() { return acceptanceId; }
    public void setAcceptanceId(Long acceptanceId) { this.acceptanceId = acceptanceId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectSummary() { return projectSummary; }
    public void setProjectSummary(String projectSummary) { this.projectSummary = projectSummary; }
    public String getCompletionStatement() { return completionStatement; }
    public void setCompletionStatement(String completionStatement) { this.completionStatement = completionStatement; }
    public String getUnfinishedItems() { return unfinishedItems; }
    public void setUnfinishedItems(String unfinishedItems) { this.unfinishedItems = unfinishedItems; }
    public String getAcceptanceNote() { return acceptanceNote; }
    public void setAcceptanceNote(String acceptanceNote) { this.acceptanceNote = acceptanceNote; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }
    public Date getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Date reviewedAt) { this.reviewedAt = reviewedAt; }
}
