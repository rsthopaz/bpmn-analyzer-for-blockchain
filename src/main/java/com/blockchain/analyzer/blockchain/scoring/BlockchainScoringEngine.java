package com.blockchain.analyzer.blockchain.scoring;

import com.blockchain.analyzer.blockchain.model.BlockchainSuitability;
import com.blockchain.analyzer.blockchain.model.ExecutionMode;
import com.blockchain.analyzer.blockchain.model.BlockchainRecommendation;
import com.blockchain.analyzer.model.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BlockchainScoringEngine {

    public BlockchainRecommendation evaluate(WorkflowNode node) {

        int score = 0;

        List<String> reasons = new ArrayList<>();

        /*
         * Financial Processes
         */
        if (node.isFinancialTask()) {

            score += 35;

            reasons.add("Financial transaction detected");

        }

        /*
         * External Collaboration
         */
        if (node.isExternalInteraction()) {

            score += 25;

            reasons.add("External interaction detected");

        }

        /*
         * Approval / Validation
         */
        if (node.isApprovalTask()) {

            score += 20;

            reasons.add("Approval or validation workflow");

        }

        /*
         * Service Automation
         */
        if ("serviceTask".equals(node.getType())) {

            score += 15;

            reasons.add("Automated service orchestration");

        }

        /*
         * Decision Workflow
         */
        if ("exclusiveGateway".equals(node.getType())) {

            score += 10;

            reasons.add("Decision workflow detected");

        }

        BlockchainSuitability suitability;

        if (score >= 70) {

            suitability = BlockchainSuitability.BLOCKCHAIN_SUITABLE;

        } else if (score >= 40) {

            suitability = BlockchainSuitability.PARTIALLY_SUITABLE;

        } else {

            suitability = BlockchainSuitability.NOT_SUITABLE;

        }

        ExecutionMode mode = determineExecutionMode(node);

        String blockchain = recommendBlockchain(node);

        return BlockchainRecommendation.builder()
                .serviceId(node.getId())
                .serviceName(node.getName())
                .serviceType(node.getType())
                .score(score)
                .suitability(suitability)
                .executionMode(mode)
                .recommendedBlockchain(blockchain)
                .reasons(reasons)
                .build();
    }

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

    private String recommendBlockchain(WorkflowNode node) {

        if (node.isFinancialTask()) {

            return "Hyperledger Fabric";

        }

        if (node.isExternalInteraction()) {

            return "Consortium Blockchain";

        }

        return "Private Blockchain";
    }
}