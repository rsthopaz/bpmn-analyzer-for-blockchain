package com.blockchain.analyzer.parser;

import com.blockchain.analyzer.model.WorkflowEdge;
import com.blockchain.analyzer.model.WorkflowGraph;
import com.blockchain.analyzer.model.WorkflowNode;

import org.springframework.stereotype.Component;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@Component
public class BPMNParser {

    public WorkflowGraph parse(String xmlContent) {

        WorkflowGraph graph = WorkflowGraph.builder().build();

        try {

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            Document document =
                    builder.parse(
                            new java.io.ByteArrayInputStream(
                                    xmlContent.getBytes()
                            )
                    );

            document.getDocumentElement().normalize();

            NodeList nodeList =
                    document.getElementsByTagName("nodes");

            for (int i = 0; i < nodeList.getLength(); i++) {

                Node node = nodeList.item(i);

                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element element = (Element) node;

                String id = getTagValue(element, "id");
                String name = getTagValue(element, "name");
                String type = getTagValue(element, "type");

                if (id == null || type == null) {
                    continue;
                }

                WorkflowNode workflowNode =
                        WorkflowNode.create(id, name, type);

                workflowNode.setExternalInteraction(
                        Boolean.parseBoolean(
                                getTagValue(
                                        element,
                                        "externalInteraction"
                                )
                        )
                );

                workflowNode.setApprovalTask(
                        Boolean.parseBoolean(
                                getTagValue(
                                        element,
                                        "approvalTask"
                                )
                        )
                );

                workflowNode.setFinancialTask(
                        Boolean.parseBoolean(
                                getTagValue(
                                        element,
                                        "financialTask"
                                )
                        )
                );

                graph.getNodes().add(workflowNode);
            }

            NodeList edgeList =
                    document.getElementsByTagName("edges");

            for (int i = 0; i < edgeList.getLength(); i++) {

                Node node = edgeList.item(i);

                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element element = (Element) node;

                String source =
                        getTagValue(element, "source");

                String target =
                        getTagValue(element, "target");

                String type =
                        getTagValue(element, "type");

                if (source == null || target == null) {
                    continue;
                }

                WorkflowEdge edge =
                        WorkflowEdge.builder()
                                .source(source)
                                .target(target)
                                .type(type)
                                .build();

                graph.getEdges().add(edge);

                graph.getNodes().stream()
                        .filter(n -> n.getId().equals(source))
                        .findFirst()
                        .ifPresent(
                                n -> n.getOutgoing().add(target)
                        );

                graph.getNodes().stream()
                        .filter(n -> n.getId().equals(target))
                        .findFirst()
                        .ifPresent(
                                n -> n.getIncoming().add(source)
                        );
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return graph;
    }

    private String getTagValue(
            Element element,
            String tagName
    ) {

        NodeList list =
                element.getElementsByTagName(tagName);

        if (list.getLength() == 0) {
            return null;
        }

        return list.item(0).getTextContent();
    }
}