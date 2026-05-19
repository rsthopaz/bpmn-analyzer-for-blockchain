package com.blockchain.analyzer.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class WorkflowNode {

    private String id;

    private String name;

    private String type;

    private List<String> incoming;

    private List<String> outgoing;

    private boolean externalInteraction;

    private boolean approvalTask;

    private boolean financialTask;

    public static WorkflowNode create(
            String id,
            String name,
            String type
    ) {

        return WorkflowNode.builder()
                .id(id)
                .name(name)
                .type(type)
                .incoming(new ArrayList<>())
                .outgoing(new ArrayList<>())
                .build();
    }
}