package com.blockchain.analyzer.parser;

import com.blockchain.analyzer.model.WorkflowEdge;
import com.blockchain.analyzer.model.WorkflowGraph;
import com.blockchain.analyzer.model.WorkflowNode;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BPMNParser {

        public WorkflowGraph parse(String input) {

        try {

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            Document document = parseDocument(builder, input);

            document.getDocumentElement().normalize();

            WorkflowGraph graph = WorkflowGraph.builder()
                    .nodes(new ArrayList<>())
                    .edges(new ArrayList<>())
                    .build();

            ParticipantMaps participantMaps = parseParticipants(document);

            parseTasks(document, graph, participantMaps.processIdToName);

            parseSequenceFlows(document, graph);

            parseMessageFlows(document, graph, participantMaps);

            return graph;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

        private Document parseDocument(DocumentBuilder builder,
                                                                   String input) throws Exception {

                if (input != null && input.trim().startsWith("<")) {
                        return builder.parse(
                                        new ByteArrayInputStream(input.getBytes())
                        );
                }

                Path path = resolvePath(input);

                return builder.parse(path.toFile());
        }

        private Path resolvePath(String filePath) {

                if (isWindows()
                        && filePath != null
                        && filePath.startsWith("/mnt/")
                        && filePath.length() > 7
                        && filePath.charAt(6) == '/') {

                        char drive = filePath.charAt(5);
                        String rest = filePath.substring(7)
                                        .replace('/', '\\');

                        return Path.of(drive + ":\\" + rest);
                }

                if (!isWindows() && isWindowsPath(filePath)) {

                        char drive = Character.toLowerCase(filePath.charAt(0));
                        String rest = filePath.substring(2)
                                        .replace('\\', '/')
                                        .replaceFirst("^/", "");

                        return Path.of("/mnt/" + drive + "/" + rest);
                }

                return Path.of(filePath);
        }

        private boolean isWindowsPath(String filePath) {

                if (filePath == null || filePath.length() < 3) {
                        return false;
                }

                char drive = filePath.charAt(0);
                char colon = filePath.charAt(1);
                char slash = filePath.charAt(2);

                return Character.isLetter(drive)
                                && colon == ':'
                                && (slash == '\\' || slash == '/');
        }

        private boolean isWindows() {

                String os = System.getProperty("os.name");

                return os != null && os.toLowerCase().contains("win");
        }

        private void parseTasks(Document document,
                                                        WorkflowGraph graph,
                                                        Map<String, String> processIdToParticipant) {

                parseElement(document, "bpmn:userTask", graph, processIdToParticipant);
                parseElement(document, "bpmn:serviceTask", graph, processIdToParticipant);
                parseElement(document, "bpmn:manualTask", graph, processIdToParticipant);
                parseElement(document, "bpmn:exclusiveGateway", graph, processIdToParticipant);
                parseElement(document, "bpmn:parallelGateway", graph, processIdToParticipant);
                parseElement(document, "bpmn:startEvent", graph, processIdToParticipant);
                parseElement(document, "bpmn:endEvent", graph, processIdToParticipant);
    }

    private void parseElement(Document document,
                              String tagName,
                                                          WorkflowGraph graph,
                                                          Map<String, String> processIdToParticipant) {

        NodeList list = document.getElementsByTagName(tagName);

        for (int i = 0; i < list.getLength(); i++) {

            Element element = (Element) list.item(i);

            String id = element.getAttribute("id");
            String name = element.getAttribute("name");

            WorkflowNode node =
                    WorkflowNode.create(id, name, tagName);

            String participantName = resolveParticipantName(
                    element,
                    processIdToParticipant
            );

            node.setParticipantName(participantName);

            enrichNode(node, name, tagName);

            graph.getNodes().add(node);
        }
    }

        private ParticipantMaps parseParticipants(Document document) {

                Map<String, String> processIdToName = new HashMap<>();
                Map<String, String> participantIdToName = new HashMap<>();

                NodeList list = document.getElementsByTagName("bpmn:participant");

                for (int i = 0; i < list.getLength(); i++) {
                        Element element = (Element) list.item(i);
                        String participantId = element.getAttribute("id");
                        String participantName = element.getAttribute("name");
                        String processRef = element.getAttribute("processRef");

                        String resolvedName = participantName == null || participantName.isBlank()
                                        ? participantId
                                        : participantName;

                        if (processRef != null && !processRef.isBlank()) {
                                processIdToName.put(processRef, resolvedName);
                        }

                        if (participantId != null && !participantId.isBlank()) {
                                participantIdToName.put(participantId, resolvedName);
                        }
                }

                return new ParticipantMaps(processIdToName, participantIdToName);
        }

        private void parseMessageFlows(
                        Document document,
                        WorkflowGraph graph,
                        ParticipantMaps participantMaps
        ) {

                Map<String, WorkflowNode> nodeById = new HashMap<>();

                for (WorkflowNode node : graph.getNodes()) {
                        nodeById.put(node.getId(), node);
                }

                NodeList list = document.getElementsByTagName("bpmn:messageFlow");

                for (int i = 0; i < list.getLength(); i++) {

                        Element element = (Element) list.item(i);

                        String sourceRef = element.getAttribute("sourceRef");
                        String targetRef = element.getAttribute("targetRef");
                        String flowId = element.getAttribute("id");

                        WorkflowNode sourceNode = nodeById.get(sourceRef);
                        WorkflowNode targetNode = nodeById.get(targetRef);

                        String sourceParticipant = resolveParticipantName(
                                        sourceNode,
                                        sourceRef,
                                        participantMaps
                        );

                        String targetParticipant = resolveParticipantName(
                                        targetNode,
                                        targetRef,
                                        participantMaps
                        );

                        if (sourceNode != null) {
                                sourceNode.setExternalDataFlow(true);
                                sourceNode.getConnectedMessageFlows()
                                                .add(formatMessageFlow(flowId, sourceParticipant, targetParticipant));
                        }

                        if (targetNode != null) {
                                targetNode.setExternalDataFlow(true);
                                targetNode.getConnectedMessageFlows()
                                                .add(formatMessageFlow(flowId, sourceParticipant, targetParticipant));
                        }

                        if (sourceNode != null
                                        && targetNode != null
                                        && sourceParticipant != null
                                        && targetParticipant != null
                                        && !sourceParticipant.equals(targetParticipant)) {

                                sourceNode.setCrossOrganizationFlow(true);
                                targetNode.setCrossOrganizationFlow(true);

                                String reason = "Message flow detected between "
                                                + sourceParticipant
                                                + " and "
                                                + targetParticipant;

                                if (sourceNode.getCrossOrganizationFlowReason() == null) {
                                        sourceNode.setCrossOrganizationFlowReason(reason);
                                }

                                if (targetNode.getCrossOrganizationFlowReason() == null) {
                                        targetNode.setCrossOrganizationFlowReason(reason);
                                }
                        }
                }
        }

        private String formatMessageFlow(
                        String flowId,
                        String sourceParticipant,
                        String targetParticipant
        ) {
                String label = flowId == null || flowId.isBlank()
                                ? "messageFlow"
                                : flowId;

                return label + ": " + sourceParticipant + " -> " + targetParticipant;
        }

        private String resolveParticipantName(
                        Element element,
                        Map<String, String> processIdToParticipant
        ) {
                Element current = element;

                while (current != null) {
                        if (isProcessElement(current)) {
                                String processId = current.getAttribute("id");
                                return processIdToParticipant.get(processId);
                        }

                        Node parent = current.getParentNode();
                        if (parent instanceof Element) {
                                current = (Element) parent;
                        } else {
                                current = null;
                        }
                }

                return null;
        }

        private String resolveParticipantName(
                        WorkflowNode node,
                        String ref,
                        ParticipantMaps participantMaps
        ) {
                if (node != null && node.getParticipantName() != null) {
                        return node.getParticipantName();
                }

                if (ref != null && participantMaps.participantIdToName.containsKey(ref)) {
                        return participantMaps.participantIdToName.get(ref);
                }

                return null;
        }

        private boolean isProcessElement(Element element) {
                String name = element.getNodeName();
                return "bpmn:process".equals(name) || name.endsWith(":process");
        }

        private static class ParticipantMaps {

                private final Map<String, String> processIdToName;
                private final Map<String, String> participantIdToName;

                private ParticipantMaps(
                                Map<String, String> processIdToName,
                                Map<String, String> participantIdToName
                ) {
                        this.processIdToName = processIdToName;
                        this.participantIdToName = participantIdToName;
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