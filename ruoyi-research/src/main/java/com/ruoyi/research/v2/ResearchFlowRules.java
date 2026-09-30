package com.ruoyi.research.v2;

import com.ruoyi.common.exception.ServiceException;

public final class ResearchFlowRules {
    private ResearchFlowRules() {}

    public static void require(boolean condition, String message) {
        if (!condition) throw new ServiceException(message);
    }

    public static String riskLevel(int probability, int impact) {
        int score = probability * impact;
        if (score >= 17) return "CRITICAL";
        if (score >= 10) return "HIGH";
        if (score >= 5) return "MEDIUM";
        return "LOW";
    }

    public static boolean editableProposal(String status) {
        return "DRAFT".equals(status) || "REVISION_REQUIRED".equals(status);
    }

    public static boolean executableProject(String status) {
        return "ACTIVE".equals(status);
    }

    public static boolean canTransitionWorkItem(String action, String status) {
        if ("start".equals(action)) return "NOT_STARTED".equals(status) || "BLOCKED".equals(status);
        if ("complete".equals(action)) return "IN_PROGRESS".equals(status) || "BLOCKED".equals(status);
        if ("block".equals(action)) return "IN_PROGRESS".equals(status);
        return false;
    }

    public static boolean canTransitionRisk(String current, String target) {
        if ("CLOSED".equals(current)) return false;
        if ("OCCURRED".equals(current)) return "CLOSED".equals(target);
        if ("OPEN".equals(current)) return "MONITORING".equals(target) || "CLOSED".equals(target);
        if ("MONITORING".equals(current)) return "OPEN".equals(target) || "CLOSED".equals(target);
        return false;
    }

    public static boolean canTransitionIssue(String current, String target) {
        if ("CLOSED".equals(current)) return false;
        if ("OPEN".equals(current)) return "IN_PROGRESS".equals(target) || "RESOLVED".equals(target) || "CLOSED".equals(target);
        if ("IN_PROGRESS".equals(current)) return "RESOLVED".equals(target) || "CLOSED".equals(target);
        if ("RESOLVED".equals(current)) return "IN_PROGRESS".equals(target) || "CLOSED".equals(target);
        return false;
    }
}
