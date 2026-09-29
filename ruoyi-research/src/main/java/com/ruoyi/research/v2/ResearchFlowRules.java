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
}
