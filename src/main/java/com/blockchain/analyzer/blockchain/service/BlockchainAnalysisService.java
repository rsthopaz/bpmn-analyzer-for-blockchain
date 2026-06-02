package com.blockchain.analyzer.blockchain.service;

import com.blockchain.analyzer.blockchain.model.BlockchainRecommendation;
import com.blockchain.analyzer.blockchain.model.AssessmentContext;
import com.blockchain.analyzer.blockchain.scoring.BlockchainScoringEngine;
import com.blockchain.analyzer.model.WorkflowGraph;
// import com.blockchain.analyzer.model.WorkflowNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlockchainAnalysisService {

    private final BlockchainScoringEngine scoringEngine;

    public BlockchainAnalysisService(BlockchainScoringEngine scoringEngine) {

        this.scoringEngine = scoringEngine;
    }

    public List<BlockchainRecommendation> analyze(WorkflowGraph graph) {

        AssessmentContext context = buildContext(graph);

        return graph.getNodes()
                .stream()
            .map(node -> scoringEngine.evaluate(node, context))
                .collect(Collectors.toList());
    }

        private AssessmentContext buildContext(WorkflowGraph graph) {

        boolean hasExternalInteraction = graph.getNodes()
            .stream()
            .anyMatch(node -> node.isExternalInteraction());

        boolean hasCrossOrganizationFlow = graph.getNodes()
            .stream()
            .anyMatch(node -> node.isCrossOrganizationFlow());

        boolean hasExternalDataFlow = graph.getNodes()
            .stream()
            .anyMatch(node -> node.isExternalDataFlow());

        return AssessmentContext.builder()
            .graphHasExternalInteraction(hasExternalInteraction)
            .graphHasCrossOrganizationFlow(hasCrossOrganizationFlow)
            .graphHasExternalDataFlow(hasExternalDataFlow)
            .build();
        }
}