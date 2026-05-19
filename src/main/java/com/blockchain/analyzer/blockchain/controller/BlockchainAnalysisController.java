package com.blockchain.analyzer.blockchain.controller;

import com.blockchain.analyzer.blockchain.model.BlockchainRecommendation;
import com.blockchain.analyzer.blockchain.service.BlockchainAnalysisService;
import com.blockchain.analyzer.model.WorkflowGraph;
import com.blockchain.analyzer.parser.BPMNParser;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
public class BlockchainAnalysisController {

    private final BPMNParser parserService;

    private final BlockchainAnalysisService analysisService;

    @PostMapping("/analyze")
    public ResponseEntity<List<BlockchainRecommendation>> analyze(
            @RequestParam("file") MultipartFile file
    ) {

        try {

            // Convert uploaded BPMN/XML file into String
            String xmlContent = new String(
                    file.getBytes(),
                    StandardCharsets.UTF_8
            );

            // Parse BPMN XML into WorkflowGraph
            WorkflowGraph graph = parserService.parse(xmlContent);

            // Analyze blockchain suitability
            List<BlockchainRecommendation> recommendations =
                    analysisService.analyze(graph);

            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {

            return ResponseEntity.internalServerError().build();
        }
    }
}