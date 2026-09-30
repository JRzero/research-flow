package com.ruoyi.research.v2;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ResearchFlowRulesTest {
    @Test void riskLevelsAreDeterministic(){
        assertEquals("LOW",ResearchFlowRules.riskLevel(1,4));
        assertEquals("MEDIUM",ResearchFlowRules.riskLevel(3,3));
        assertEquals("HIGH",ResearchFlowRules.riskLevel(4,4));
        assertEquals("CRITICAL",ResearchFlowRules.riskLevel(5,4));
    }
    @Test void proposalEditabilityIsExplicit(){
        assertTrue(ResearchFlowRules.editableProposal("DRAFT"));
        assertTrue(ResearchFlowRules.editableProposal("REVISION_REQUIRED"));
        assertFalse(ResearchFlowRules.editableProposal("UNDER_REVIEW"));
    }
    @Test void workItemTransitionsAreExplicit(){
        assertTrue(ResearchFlowRules.canTransitionWorkItem("start","NOT_STARTED"));
        assertTrue(ResearchFlowRules.canTransitionWorkItem("complete","IN_PROGRESS"));
        assertFalse(ResearchFlowRules.canTransitionWorkItem("start","DONE"));
    }

    @Test void riskTransitionsDoNotReopenClosedOrOccurredRisks(){
        assertTrue(ResearchFlowRules.canTransitionRisk("OPEN","MONITORING"));
        assertTrue(ResearchFlowRules.canTransitionRisk("OCCURRED","CLOSED"));
        assertFalse(ResearchFlowRules.canTransitionRisk("CLOSED","OPEN"));
        assertFalse(ResearchFlowRules.canTransitionRisk("OCCURRED","OPEN"));
    }

    @Test void issueTransitionsAreControlled(){
        assertTrue(ResearchFlowRules.canTransitionIssue("OPEN","IN_PROGRESS"));
        assertTrue(ResearchFlowRules.canTransitionIssue("RESOLVED","CLOSED"));
        assertFalse(ResearchFlowRules.canTransitionIssue("CLOSED","IN_PROGRESS"));
    }
}
