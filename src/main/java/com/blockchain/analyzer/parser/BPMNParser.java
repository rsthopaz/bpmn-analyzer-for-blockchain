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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            parseTasks(document, graph);

            parseSequenceFlows(document, graph);

            deriveContextFlags(graph);

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

        private void deriveContextFlags(WorkflowGraph graph) {

                Map<String, WorkflowNode> nodesById = new HashMap<>();

                for (WorkflowNode node : graph.getNodes()) {
                        nodesById.put(node.getId(), node);
                }

                Map<String, List<String>> adjacency = new HashMap<>();

                for (WorkflowEdge edge : graph.getEdges()) {
                        adjacency
                                        .computeIfAbsent(edge.getSource(), key -> new ArrayList<>())
                                        .add(edge.getTarget());
                }

                for (WorkflowNode node : graph.getNodes()) {
                        ContextAggregation aggregation = collectReachableContext(
                                        node.getId(),
                                        nodesById,
                                        adjacency
                        );

                        node.setExternalDataFlow(aggregation.hasExternalInteraction);
                        node.setCrossOrganizationFlow(
                                        aggregation.participantTags.size() >= 2
                        );
                }
        }

        private ContextAggregation collectReachableContext(
                        String startId,
                        Map<String, WorkflowNode> nodesById,
                        Map<String, List<String>> adjacency
        ) {
                Set<String> visited = new HashSet<>();
                Set<String> participantTags = new HashSet<>();
                boolean hasExternalInteraction = false;

                Deque<String> stack = new ArrayDeque<>();
                stack.push(startId);

                while (!stack.isEmpty()) {
                        String currentId = stack.pop();

                        if (!visited.add(currentId)) {
                                continue;
                        }

                        WorkflowNode currentNode = nodesById.get(currentId);

                        if (currentNode != null) {
                                participantTags.addAll(extractParticipantTags(currentNode));

                                if (currentNode.isExternalInteraction()) {
                                        hasExternalInteraction = true;
                                }
                        }

                        for (String nextId : adjacency.getOrDefault(currentId, List.of())) {
                                stack.push(nextId);
                        }
                }

                return new ContextAggregation(participantTags, hasExternalInteraction);
        }

        private Set<String> extractParticipantTags(WorkflowNode node) {

                Set<String> tags = new HashSet<>();

                String lower = (node.getName() + " " + node.getType()).toLowerCase();

                if (lower.contains("customer")) {
                        tags.add("customer");
                }

                if (lower.contains("supplier") || lower.contains("vendor")) {
                        tags.add("supplier");
                }

                if (lower.contains("partner")) {
                        tags.add("partner");
                }

                if (lower.contains("regulator") || lower.contains("auditor")) {
                        tags.add("regulator");
                }

                if (lower.contains("shipper") || lower.contains("logistics")) {
                        tags.add("shipper");
                }

                if (node.isExternalInteraction()) {
                        tags.add("external");
                }

                return tags;
        }

        private static class ContextAggregation {

                private final Set<String> participantTags;

                private final boolean hasExternalInteraction;

                private ContextAggregation(
                                Set<String> participantTags,
                                boolean hasExternalInteraction
                ) {
                        this.participantTags = participantTags;
                        this.hasExternalInteraction = hasExternalInteraction;
                }
        }
}