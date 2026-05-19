package com.blockchain.analyzer.controller;

import com.blockchain.analyzer.model.BPMNTask;
import com.blockchain.analyzer.service.BPMNAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bpmn")
@RequiredArgsConstructor
public class BPMNController {

    private final BPMNAnalysisService analysisService;

    @GetMapping("/analyze")
    public List<BPMNTask> analyze(
            @RequestParam String filePath
    ) {

        return analysisService.analyze(filePath);
    }
}