package com.blockchain.analyzer.parser;

import com.blockchain.analyzer.blockchain.scoring.BlockchainScoringEngine;
import com.blockchain.analyzer.blockchain.model.BlockchainRecommendation;
import com.blockchain.analyzer.model.WorkflowGraph;
import com.blockchain.analyzer.model.WorkflowNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BPMNParserTest {

    @Test
    void parseMessageFlowEnablesExternalDataFlowAndCrossOrganizationFlow() {
        String xml = "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
                + "xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">"
                + "<process id=\"process_1\">"
                + "<laneSet>"
                + "<lane id=\"lane_customer\" name=\"Customer\">"
                + "<flowNodeRef>task_1</flowNodeRef>"
                + "</lane>"
                + "<lane id=\"lane_supplier\" name=\"Supplier\">"
                + "<flowNodeRef>task_2</flowNodeRef>"
                + "</lane>"
                + "</laneSet>"
                + "<bpmn:userTask id=\"task_1\" name=\"Receive Customer Order\"/>"
                + "<bpmn:serviceTask id=\"task_2\" name=\"Send Order Rejection Message to Customer\"/>"
                + "<bpmn:messageFlow id=\"flow_1\" sourceRef=\"task_2\" targetRef=\"task_1\"/>"
                + "</process>"
                + "</definitions>";

        BPMNParser parser = new BPMNParser();
        WorkflowGraph graph = parser.parse(xml);

        assertNotNull(graph);
        assertEquals(2, graph.getNodes().size());

        WorkflowNode task1 = graph.getNodes().stream()
                .filter(node -> "task_1".equals(node.getId()))
                .findFirst()
                .orElseThrow();
        WorkflowNode task2 = graph.getNodes().stream()
                .filter(node -> "task_2".equals(node.getId()))
                .findFirst()
                .orElseThrow();

        assertTrue(task1.isExternalDataFlow(), "task_1 should have external data flow enabled");
        assertTrue(task1.isCrossOrganizationFlow(), "task_1 should have cross-organization flow enabled");
        assertTrue(task2.isExternalDataFlow(), "task_2 should have external data flow enabled");
        assertTrue(task2.isCrossOrganizationFlow(), "task_2 should have cross-organization flow enabled");
    }

    @Test
    void recommendBlockchainWhenMessageFlowAndExternalInteractionExists() {
        String xml = "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
                + "xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">"
                + "<process id=\"process_1\">"
                + "<laneSet>"
                + "<lane id=\"lane_customer\" name=\"Customer\">"
                + "<flowNodeRef>task_1</flowNodeRef>"
                + "</lane>"
                + "<lane id=\"lane_supplier\" name=\"Supplier\">"
                + "<flowNodeRef>task_2</flowNodeRef>"
                + "</lane>"
                + "</laneSet>"
                + "<bpmn:userTask id=\"task_1\" name=\"Receive Customer Order\"/>"
                + "<bpmn:serviceTask id=\"task_2\" name=\"Process Payment\"/>"
                + "<bpmn:messageFlow id=\"flow_1\" sourceRef=\"task_2\" targetRef=\"task_1\"/>"
                + "</process>"
                + "</definitions>";

        BPMNParser parser = new BPMNParser();
        WorkflowGraph graph = parser.parse(xml);

        BlockchainScoringEngine engine = new BlockchainScoringEngine();
        List<BlockchainRecommendation> recommendations = graph.getNodes().stream()
                .map(engine::evaluate)
                .toList();

        assertEquals(2, recommendations.size());

        BlockchainRecommendation recommendation = recommendations.stream()
                .filter(r -> "task_1".equals(r.getServiceId()))
                .findFirst()
                .orElseThrow();

        assertTrue(recommendation.isHardGatePassed(), "Hard gate should pass for task_1");
        assertNotEquals(0, recommendation.getScore(), "Score should be greater than zero for suitable task");
        assertNotEquals("Not applicable", recommendation.getRecommendedBlockchain());
    }
}
