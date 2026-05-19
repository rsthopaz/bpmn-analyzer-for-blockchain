package com.blockchain.analyzer.parser;

import com.blockchain.analyzer.model.WorkflowEdge;
import com.blockchain.analyzer.model.WorkflowGraph;
import com.blockchain.analyzer.model.WorkflowNode;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;

@Component
public class BPMNParser {

    public WorkflowGraph parse(String filePath) {

        try {

            File file = new File(filePath);

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            Document document = builder.parse(file);

            document.getDocumentElement().normalize();

            WorkflowGraph graph = WorkflowGraph.builder()
                    .nodes(new ArrayList<>())
                    .edges(new ArrayList<>())
                    .build();

            parseTasks(document, graph);

            parseSequenceFlows(document, graph);

            return graph;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void parseTasks(Document document,
                            WorkflowGraph graph) {

        parseElement(document, "bpmn:userTask", graph);
        parseElement(document, "bpmn:serviceTask", graph);
        parseElement(document, "bpmn:manualTask", graph);
        parseElement(document, "bpmn:exclusiveGateway", graph);
        parseElement(document, "bpmn:parallelGateway", graph);
        parseElement(document, "bpmn:startEvent", graph);
        parseElement(document, "bpmn:endEvent", graph);
    }

    private void parseElement(Document document,
                              String tagName,
                              WorkflowGraph graph) {

        NodeList list = document.getElementsByTagName(tagName);

        for (int i = 0; i < list.getLength(); i++) {

            Element element = (Element) list.item(i);

            String id = element.getAttribute("id");
            String name = element.getAttribute("name");

            WorkflowNode node =
                    WorkflowNode.create(id, name, tagName);

            enrichNode(node, name, tagName);

            graph.getNodes().add(node);
        }
    }

    private void enrichNode(WorkflowNode node,
                            String name,
                            String type) {

        String lower =
                (name + " " + type).toLowerCase();

        node.setApprovalTask(
                lower.contains("approve")
                        || lower.contains("validation")
                        || lower.contains("confirm")
        );

        node.setFinancialTask(
                lower.contains("payment")
                        || lower.contains("invoice")
                        || lower.contains("billing")
                        || lower.contains("transaction")
        );

        node.setExternalInteraction(
                lower.contains("customer")
                        || lower.contains("supplier")
                        || lower.contains("shipment")
                        || lower.contains("delivery")
                        || lower.contains("message")
        );
    }

    private void parseSequenceFlows(Document document,
                                    WorkflowGraph graph) {

        NodeList list =
                document.getElementsByTagName("bpmn:sequenceFlow");

        for (int i = 0; i < list.getLength(); i++) {

            Element element = (Element) list.item(i);

            WorkflowEdge edge = WorkflowEdge.builder()
                    .source(element.getAttribute("sourceRef"))
                    .target(element.getAttribute("targetRef"))
                    .type("sequenceFlow")
                    .build();

            graph.getEdges().add(edge);
        }
    }
}