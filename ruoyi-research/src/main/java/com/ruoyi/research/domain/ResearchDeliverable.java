package com.ruoyi.research.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class ResearchDeliverable extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long deliverableId;
    private Long projectId;
    private String name;
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date completedDate;
    private String description;
    public Long getDeliverableId() { return deliverableId; }
    public void setDeliverableId(Long deliverableId) { this.deliverableId = deliverableId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Date getCompletedDate() { return completedDate; }
    public void setCompletedDate(Date completedDate) { this.completedDate = completedDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
