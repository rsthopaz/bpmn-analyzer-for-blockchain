package com.blockchain.analyzer.controller;

import com.blockchain.analyzer.model.WorkflowGraph;
import com.blockchain.analyzer.service.BPMNAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bpmn")
@RequiredArgsConstructor
public class BPMNController {

    private final BPMNAnalysisService analysisService;

    @GetMapping("/analyze")
    public WorkflowGraph analyze(
            @RequestParam String filePath
    ) {

        return analysisService.analyze(filePath);
    }
}