package com.blockchain.analyzer.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WorkflowGraph {

    private List<WorkflowNode> nodes;

    private List<WorkflowEdge> edges;

}