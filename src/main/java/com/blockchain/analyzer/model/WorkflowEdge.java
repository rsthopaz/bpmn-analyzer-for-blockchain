package com.blockchain.analyzer.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowEdge {

    private String source;

    private String target;

    private String type;

}