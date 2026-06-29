package com.blockchain.analyzer.blockchain.scoring;

import com.blockchain.analyzer.blockchain.model.BlockchainSuitability;
import com.blockchain.analyzer.blockchain.model.AssessmentContext;
import com.blockchain.analyzer.blockchain.model.ExecutionMode;
import com.blockchain.analyzer.blockchain.model.BlockchainRecommendation;
import com.blockchain.analyzer.model.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BlockchainScoringEngine {

    public BlockchainRecommendation evaluate(WorkflowNode node) {

        return evaluate(node, AssessmentContext.builder().build());
    }

    public BlockchainRecommendation evaluate(
            WorkflowNode node,
            AssessmentContext context
    ) {

        int score = 0;

        List<String> reasons = new ArrayList<>();
        List<String> gateReasons = new ArrayList<>();
        List<String> stageSummaries = new ArrayList<>();

        WustDecision decision = evaluateWustCriteria(node, context);
        boolean gatePassed = passesHardGate(decision, gateReasons);

        stageSummaries.add(
            "Stage 1 (Wust gate): " + (gatePassed ? "PASS" : "FAIL")
        );

        stageSummaries.add(buildContextSummary(node));

        if (gatePassed) {
            score = calculateSoftScore(node, reasons);
        } else {
            score = 0;
        }

        BlockchainSuitability suitability;

        if (!gatePassed) {
            suitability = BlockchainSuitability.NOT_SUITABLE;
        } else if (score >= 70) {
            suitability = BlockchainSuitability.BLOCKCHAIN_SUITABLE;
        } else if (score >= 40) {
            suitability = BlockchainSuitability.PARTIALLY_SUITABLE;
        } else {
            suitability = BlockchainSuitability.NOT_SUITABLE;
        }

        ExecutionMode mode = gatePassed
                ? determineExecutionMode(node)
                : ExecutionMode.OFF_CHAIN;

        String blockchain = gatePassed
            ? recommendBlockchain(decision)
            : "Not applicable";

        stageSummaries.add(
            "Stage 3 (Scoring): score=" + score
                + ", suitability=" + suitability
                + ", blockchain=" + blockchain
        );

        return BlockchainRecommendation.builder()
                .serviceId(node.getId())
                .serviceName(node.getName())
                .serviceType(node.getType())
                .score(score)
                .suitability(suitability)
                .hardGatePassed(gatePassed)
                .executionMode(mode)
                .recommendedBlockchain(blockchain)
                .gateReasons(gateReasons)
            .stageSummaries(stageSummaries)
                .reasons(reasons)
                .build();
    }

        private boolean passesHardGate(
            WustDecision decision,
            List<String> gateReasons
        ) {

        if (!decision.sharedLedger) {
            gateReasons.add(
                "Wust gate: no shared ledger requirement detected"
            );
            return false;
        }

        if (!decision.multipleWriters) {
            gateReasons.add(
                "Wust gate: no multiple writers detected"
            );
            return false;
        }

        if (!decision.untrustedStakeholders && !decision.dataPrivate) {
            gateReasons.add(
                "Wust gate: trusted stakeholders with no private data need"
            );
            return false;
        }

        gateReasons.add("Wust gate: shared ledger requirement met");
        gateReasons.add("Wust gate: multiple writers detected");
        gateReasons.add("Wust gate: untrusted stakeholders or private data need");

        return true;
        }

        private WustDecision evaluateWustCriteria(
            WorkflowNode node,
            AssessmentContext context
        ) {

            boolean sharedLedger =
                node.isExternalDataFlow()
                    || node.isCrossOrganizationFlow();

            boolean multipleWriters = node.isCrossOrganizationFlow();

            boolean untrustedStakeholders = node.isExternalInteraction();

            // Treat processes involving financial/approval tasks or sensitive personal/medical data as private
            boolean dataPrivate = node.isFinancialTask()
                    || node.isApprovalTask()
                    || containsSensitiveData(node.getName());

        boolean restrictedControl = dataPrivate;

        boolean consortiumMaintenance =
            restrictedControl && node.isCrossOrganizationFlow();

        return new WustDecision(
            sharedLedger,
            multipleWriters,
            untrustedStakeholders,
            dataPrivate,
            restrictedControl,
            consortiumMaintenance
        );
        }

    private boolean containsSensitiveData(String name) {
        if (name == null) return false;

        String lower = name.toLowerCase();

        return lower.contains("patient")
                || lower.contains("medical")
                || lower.contains("health")
                || lower.contains("record")
                || lower.contains("ssn")
                || lower.contains("identity")
                || lower.contains("personal")
                || lower.contains("referral")
                || lower.contains("registration");
    }

    private String buildContextSummary(WorkflowNode node) {

        return "Stage 2 (Context): externalInteraction="
                + node.isExternalInteraction()
                + ", crossOrganizationFlow="
                + node.isCrossOrganizationFlow()
                + ", externalDataFlow="
                + node.isExternalDataFlow()
                + ", financialTask="
                + node.isFinancialTask()
                + ", approvalTask="
                + node.isApprovalTask();
    }

    private int calculateSoftScore(
            WorkflowNode node,
            List<String> reasons
    ) {

        Map<SoftCriterion, Double> weights = computeWeights();

        int score = 0;

        score += applyCriterion(
                node.isFinancialTask(),
                SoftCriterion.FINANCIAL_PROCESS,
                weights,
                reasons,
                "Financial transaction detected"
        );

        score += applyCriterion(
                node.isExternalInteraction(),
                SoftCriterion.EXTERNAL_INTERACTION,
                weights,
                reasons,
                "External interaction detected"
        );

        score += applyCriterion(
                node.isApprovalTask(),
                SoftCriterion.APPROVAL_WORKFLOW,
                weights,
                reasons,
                "Approval or validation workflow"
        );

        score += applyCriterion(
                node.getType() != null && node.getType().endsWith("serviceTask"),
                SoftCriterion.SERVICE_AUTOMATION,
                weights,
                reasons,
                "Automated service orchestration"
        );

        score += applyCriterion(
                node.getType() != null && node.getType().endsWith("exclusiveGateway"),
                SoftCriterion.DECISION_WORKFLOW,
                weights,
                reasons,
                "Decision workflow detected"
        );

        score += applyCriterion(
                node.isCrossOrganizationFlow(),
                SoftCriterion.CROSS_ORGANIZATION_FLOW,
                weights,
                reasons,
                "Cross-organization flow context"
        );

        score += applyCriterion(
                node.isExternalDataFlow(),
                SoftCriterion.EXTERNAL_DATA_FLOW,
                weights,
                reasons,
                "External data flow context"
        );

        return Math.min(score, 100);
    }

    private int applyCriterion(
            boolean applies,
            SoftCriterion criterion,
            Map<SoftCriterion, Double> weights,
            List<String> reasons,
            String reason
    ) {

        if (!applies) {
            return 0;
        }

        double weight = weights.getOrDefault(criterion, 0.0);
        int points = (int) Math.round(weight * 100);

        reasons.add(reason + " (weight=" + formatWeight(weight) + ")");

        return points;
    }

    private Map<SoftCriterion, Double> computeWeights() {

        Map<SoftCriterion, Double> geometricMeans =
                new EnumMap<>(SoftCriterion.class);

        int size = SoftCriterion.values().length;

        for (int i = 0; i < size; i++) {
            double product = 1.0;
            for (int j = 0; j < size; j++) {
                product *= AHP_PAIRWISE[i][j];
            }
            geometricMeans.put(
                    SoftCriterion.values()[i],
                    Math.pow(product, 1.0 / size)
            );
        }

        double sum = geometricMeans.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        Map<SoftCriterion, Double> weights =
                new EnumMap<>(SoftCriterion.class);

        for (Map.Entry<SoftCriterion, Double> entry : geometricMeans.entrySet()) {
            weights.put(entry.getKey(), entry.getValue() / sum);
        }

        return weights;
    }

    private String formatWeight(double weight) {

        return String.format("%.2f", weight);
    }

    private enum SoftCriterion {
        FINANCIAL_PROCESS,
        EXTERNAL_INTERACTION,
        APPROVAL_WORKFLOW,
        SERVICE_AUTOMATION,
        DECISION_WORKFLOW,
        CROSS_ORGANIZATION_FLOW,
        EXTERNAL_DATA_FLOW
    }

    private static class WustDecision {

        private final boolean sharedLedger;
        private final boolean multipleWriters;
        private final boolean untrustedStakeholders;
        private final boolean dataPrivate;
        private final boolean restrictedControl;
        private final boolean consortiumMaintenance;

        private WustDecision(
                boolean sharedLedger,
                boolean multipleWriters,
                boolean untrustedStakeholders,
                boolean dataPrivate,
                boolean restrictedControl,
                boolean consortiumMaintenance
        ) {
            this.sharedLedger = sharedLedger;
            this.multipleWriters = multipleWriters;
            this.untrustedStakeholders = untrustedStakeholders;
            this.dataPrivate = dataPrivate;
            this.restrictedControl = restrictedControl;
            this.consortiumMaintenance = consortiumMaintenance;
        }
    }

    private static final double[][] AHP_PAIRWISE = {
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1}
    };

    private ExecutionMode determineExecutionMode(WorkflowNode node) {

        if (node.isFinancialTask()) {

            return ExecutionMode.HYBRID;

        }

        if (node.isApprovalTask()) {

            return ExecutionMode.ON_CHAIN;

        }

        if ("manualTask".equals(node.getType())) {

            return ExecutionMode.OFF_CHAIN;

        }

        return ExecutionMode.HYBRID;
    }

    private String recommendBlockchain(WustDecision decision) {

        if (!decision.dataPrivate && !decision.restrictedControl) {
            return "Public Blockchain";
        }

        if (decision.consortiumMaintenance) {
            return "Consortium Blockchain";
        }

        return "Private Blockchain";
    }
}