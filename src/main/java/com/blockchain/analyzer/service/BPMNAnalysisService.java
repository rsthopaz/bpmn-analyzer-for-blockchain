package com.blockchain.analyzer.service;

import com.blockchain.analyzer.model.WorkflowGraph;
import com.blockchain.analyzer.parser.BPMNParser;
import org.springframework.stereotype.Service;

@Service
public class BPMNAnalysisService {

    public WorkflowGraph analyze(String filePath) {

        BPMNParser parser = new BPMNParser();

        return parser.parse(filePath);
    }
}