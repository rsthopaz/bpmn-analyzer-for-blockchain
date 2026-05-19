package com.blockchain.analyzer.controller;

import com.blockchain.analyzer.blockchain.model.BlockchainRecommendation;
import com.blockchain.analyzer.blockchain.service.BlockchainAnalysisService;
import com.blockchain.analyzer.parser.model.WorkflowGraph;
import com.blockchain.analyzer.parser.service.BpmnParserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainAnalysisController {

    private final BpmnParserService parserService;

    private final BlockchainAnalysisService analysisService;

    public BlockchainAnalysisController(
            BpmnParserService parserService,
            BlockchainAnalysisService analysisService
    ) {

        this.parserService = parserService;
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public List<BlockchainRecommendation> analyze(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        WorkflowGraph graph = parserService.parse(file);

        return analysisService.analyze(graph);
    }
}