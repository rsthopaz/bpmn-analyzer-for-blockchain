package com.blockchain.analyzer.parser;

import com.blockchain.analyzer.model.WorkflowEdge;
import com.blockchain.analyzer.model.WorkflowGraph;
import com.blockchain.analyzer.model.WorkflowNode;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BPMNParser {

    public WorkflowGraph parse(String filePath) {

        List<WorkflowNode> nodes = new ArrayList<>();
        List<WorkflowEdge> edges = new ArrayList<>();

        BpmnModelInstance modelInstance =
                Bpmn.readModelFromFile(new File(filePath));

        Collection<FlowNode> flowNodes =
                modelInstance.getModelElementsByType(FlowNode.class);

        for (FlowNode node : flowNodes) {

            WorkflowNode workflowNode =
                    WorkflowNode.create(
                            node.getId(),
                            node.getName(),
                            node.getElementType().getTypeName()
                    );

            workflowNode.setApprovalTask(
                    containsApprovalKeyword(node.getName())
            );

            workflowNode.setExternalInteraction(
                    containsExternalKeyword(node.getName())
            );

            workflowNode.setFinancialTask(
                    containsFinancialKeyword(node.getName())
            );

            for (SequenceFlow incoming :
                    node.getIncoming()) {

                workflowNode.getIncoming()
                        .add(incoming.getSource().getId());
            }

            for (SequenceFlow outgoing :
                    node.getOutgoing()) {

                workflowNode.getOutgoing()
                        .add(outgoing.getTarget().getId());

                edges.add(
                        WorkflowEdge.builder()
                                .source(
                                        outgoing.getSource().getId()
                                )
                                .target(
                                        outgoing.getTarget().getId()
                                )
                                .type("sequenceFlow")
                                .build()
                );
            }

            nodes.add(workflowNode);
        }

        return WorkflowGraph.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    private boolean containsApprovalKeyword(String text) {

        if (text == null) return false;

        text = text.toLowerCase();

        return text.contains("approve")
                || text.contains("validate")
                || text.contains("confirm");
    }

    private boolean containsExternalKeyword(String text) {

        if (text == null) return false;

        text = text.toLowerCase();

        return text.contains("payment")
                || text.contains("invoice")
                || text.contains("shipment")
                || text.contains("delivery");
    }

    private boolean containsFinancialKeyword(String text) {

        if (text == null) return false;

        text = text.toLowerCase();

        return text.contains("payment")
                || text.contains("invoice")
                || text.contains("billing");
    }
}