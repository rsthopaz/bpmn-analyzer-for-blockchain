package com.blockchain.analyzer.service;

import com.blockchain.analyzer.model.BPMNTask;
import com.blockchain.analyzer.parser.BPMNParser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BPMNAnalysisService {

    public List<BPMNTask> analyze(String filePath) {

        BPMNParser parser = new BPMNParser();

        return parser.parse(filePath);
    }
}