package com.blockchain.analyzer.blockchain.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BlockchainRecommendation {

    private String serviceId;

    private String serviceName;

    private String serviceType;

    private BlockchainSuitability suitability;

    private int score;

    private boolean hardGatePassed;

    private ExecutionMode executionMode;

    private String recommendedBlockchain;

    private List<String> gateReasons;

    private List<String> stageSummaries;

    private List<String> reasons;

}