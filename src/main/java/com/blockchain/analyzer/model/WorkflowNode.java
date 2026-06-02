package com.blockchain.analyzer.model;

import lombok.*;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowNode {

    private String id;

    private String name;

    private String type;

    @Builder.Default
    private List<String> incoming = new ArrayList<>();

    @Builder.Default
    private List<String> outgoing = new ArrayList<>();

    private boolean externalInteraction;

    private boolean approvalTask;

    private boolean financialTask;

    private boolean crossOrganizationFlow;

    private boolean externalDataFlow;

    public static WorkflowNode create(
            String id,
            String name,
            String type
    ) {
        return WorkflowNode.builder()
                .id(id)
                .name(name)
                .type(type)
                .build();
    }
}