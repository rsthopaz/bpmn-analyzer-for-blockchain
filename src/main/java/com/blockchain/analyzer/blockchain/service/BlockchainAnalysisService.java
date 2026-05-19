package com.blockchain.analyzer.blockchain.service;

import com.blockchain.analyzer.blockchain.model.BlockchainRecommendation;
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

        return graph.getNodes()
                .stream()
                .map(scoringEngine::evaluate)
                .collect(Collectors.toList());
    }
}