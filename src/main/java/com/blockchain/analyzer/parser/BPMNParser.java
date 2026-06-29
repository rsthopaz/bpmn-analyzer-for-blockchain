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

                // Detect XPDL package root and handle basic XPDL -> internal graph mapping
                String rootLocal = document.getDocumentElement().getLocalName();

                WorkflowGraph graph = WorkflowGraph.builder()
                    .nodes(new ArrayList<>())
                    .edges(new ArrayList<>())
                    .build();

                if ("Package".equals(rootLocal)) {
                parseXPDL(document, graph);
                inferFlowFlags(graph);
                return graph;
                }

                // Default: assume BPMN XML
                parseTasks(document, graph);
                parseParticipants(document, graph);
                parseMessageFlows(document, graph);
                parseSequenceFlows(document, graph);
                inferFlowFlags(graph);

                // deriveContextFlags(graph);

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
        parseElement(document, "bpmn:sendTask", graph);
        parseElement(document, "bpmn:receiveTask", graph);
        parseElement(document, "bpmn:exclusiveGateway", graph);
        parseElement(document, "bpmn:parallelGateway", graph);
        parseElement(document, "bpmn:startEvent", graph);
        parseElement(document, "bpmn:endEvent", graph);
    }

    private void parseElement(Document document,
                              String tagName,
                              WorkflowGraph graph) {

        NodeList list = getElementsByTagName(document, tagName);

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
                        || lower.contains("order")
                        || lower.contains("send")
                        || lower.contains("receive")
                        || lower.contains("callback")
        );

        node.setExternalDataFlow(
                lower.contains("send")
                        || lower.contains("receive")
                        || lower.contains("message")
                        || lower.contains("request")
                        || lower.contains("notify")
                        || lower.contains("call")
                        || lower.contains("data")
                        || lower.contains("order")
        );

        node.setCrossOrganizationFlow(
                lower.contains("customer")
                        || lower.contains("supplier")
                        || lower.contains("order management")
                        || lower.contains("billing")
                        || lower.contains("inventory management")
                        || lower.contains("shipping")
                        || lower.contains("delivery")
                        || lower.contains("payment")
                        || lower.contains("message")
                        || lower.contains("request")
                        || lower.contains("send")
                        || lower.contains("receive")
        );

                // Additional healthcare / referral related keywords to detect external stakeholders
                if (!node.isExternalInteraction()) {
                    node.setExternalInteraction(
                        lower.contains("referral")
                            || lower.contains("patient")
                            || lower.contains("hospital")
                            || lower.contains("clinic")
                            || lower.contains("provider")
                            || lower.contains("doctor")
                            || lower.contains("nurse")
                            || lower.contains("registration")
                            || lower.contains("medical")
                    );
                }

                if (!node.isCrossOrganizationFlow()) {
                    node.setCrossOrganizationFlow(
                        lower.contains("referral")
                            || lower.contains("patient")
                            || lower.contains("hospital")
                            || lower.contains("clinic")
                            || lower.contains("provider")
                    );
                }
    }

    private void parseMessageFlows(Document document,
                                   WorkflowGraph graph) {

        NodeList list = getElementsByTagName(document, "bpmn:messageFlow");

        for (int i = 0; i < list.getLength(); i++) {

            Element element = (Element) list.item(i);

            String source = element.getAttribute("sourceRef");
            String target = element.getAttribute("targetRef");

            WorkflowEdge edge = WorkflowEdge.builder()
                    .source(source)
                    .target(target)
                    .type("messageFlow")
                    .build();

            graph.getEdges().add(edge);

            WorkflowNode sourceNode = findNodeById(graph, source);
            WorkflowNode targetNode = findNodeById(graph, target);

            if (sourceNode != null) {
                sourceNode.getConnectedMessageFlows().add(target);
            }
            if (targetNode != null) {
                targetNode.getConnectedMessageFlows().add(source);
            }
        }
    }

    private void parseSequenceFlows(Document document,
                                    WorkflowGraph graph) {

        NodeList list = getElementsByTagName(document, "bpmn:sequenceFlow");

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

    private void parseXPDL(Document document,
                           WorkflowGraph graph) {
        // Map Pools -> Process id (Pool.Process attribute)
        Map<String, String> processToPool = new HashMap<>();
        NodeList pools = getElementsByTagName(document, "Pool");

        for (int i = 0; i < pools.getLength(); i++) {
            Element pool = (Element) pools.item(i);
            String processRef = pool.getAttribute("Process");
            String poolName = pool.getAttribute("Name");
            if (processRef != null && !processRef.isEmpty()) {
                processToPool.put(processRef, poolName);
            }
        }

        // Find WorkflowProcesses that reference external data (DataStoreReferences / DataAssociations)
        Set<String> processesWithExternal = new HashSet<>();
        NodeList workflowProcesses = getElementsByTagName(document, "WorkflowProcess");

        for (int i = 0; i < workflowProcesses.getLength(); i++) {
            Element wp = (Element) workflowProcesses.item(i);
            String pid = wp.getAttribute("Id");

            NodeList dsRefs = getElementsByTagName(wp, "DataStoreReference");
            NodeList dataAssocs = getElementsByTagName(wp, "DataAssociation");

            if ((dsRefs != null && dsRefs.getLength() > 0)
                    || (dataAssocs != null && dataAssocs.getLength() > 0)) {
                processesWithExternal.add(pid);
            }
        }

        // Activities -> nodes
        NodeList activities = getElementsByTagName(document, "Activity");

        for (int i = 0; i < activities.getLength(); i++) {
            Element act = (Element) activities.item(i);
            String id = act.getAttribute("Id");
            String name = act.getAttribute("Name");

            WorkflowNode node = WorkflowNode.create(id, name, "xpdl:activity");
            enrichNode(node, name, "xpdl:activity");

            // Attempt to determine containing WorkflowProcess for participant assignment
            String participant = null;
            Node parent = act.getParentNode();
            while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                Element p = (Element) parent;
                if ("WorkflowProcess".equals(p.getLocalName()) || "WorkflowProcess".equals(p.getNodeName())) {
                    String pid = p.getAttribute("Id");
                    participant = processToPool.get(pid);

                    if (processesWithExternal.contains(pid)) {
                        node.setExternalDataFlow(true);
                        node.setCrossOrganizationFlow(true);
                    }

                    break;
                }
                parent = parent.getParentNode();
            }

            if (participant != null && !participant.isEmpty()) {
                node.setParticipantName(participant);
            }

            graph.getNodes().add(node);
        }

        // Transitions -> sequence flows
        NodeList transitions = getElementsByTagName(document, "Transition");

        for (int i = 0; i < transitions.getLength(); i++) {
            Element t = (Element) transitions.item(i);
            String from = t.getAttribute("From");
            String to = t.getAttribute("To");

            WorkflowEdge edge = WorkflowEdge.builder()
                    .source(from)
                    .target(to)
                    .type("sequenceFlow")
                    .build();

            graph.getEdges().add(edge);
        }
    }

    private void inferFlowFlags(WorkflowGraph graph) {

        for (WorkflowNode node : graph.getNodes()) {

            if (!node.getConnectedMessageFlows().isEmpty()) {
                node.setExternalDataFlow(true);
                node.setCrossOrganizationFlow(true);
            }

            for (String connectedId : node.getConnectedMessageFlows()) {
                WorkflowNode connected = findNodeById(graph, connectedId);

                if (connected == null) {
                    continue;
                }

                String nodeParticipant = node.getParticipantName();
                String connectedParticipant = connected.getParticipantName();

                if (nodeParticipant != null
                        && connectedParticipant != null
                        && !nodeParticipant.isEmpty()
                        && !connectedParticipant.isEmpty()
                        && !nodeParticipant.equals(connectedParticipant)) {

                    node.setCrossOrganizationFlow(true);
                    connected.setCrossOrganizationFlow(true);
                }
            }
        }
    }

    private WorkflowNode findNodeById(WorkflowGraph graph,
                                      String nodeId) {

        return graph.getNodes()
                .stream()
                .filter(node -> nodeId.equals(node.getId()))
                .findFirst()
                .orElse(null);
    }

    private NodeList getElementsByTagName(Document document,
                                         String tagName) {

        NodeList list = document.getElementsByTagName(tagName);

        if (list.getLength() > 0) {
            return list;
        }

        String localName = tagName.contains(":")
                ? tagName.substring(tagName.indexOf(":") + 1)
                : tagName;

        return document.getElementsByTagName(localName);
    }

    private void parseParticipants(Document document,
                                   WorkflowGraph graph) {

        NodeList laneList = getElementsByTagName(document, "bpmn:lane");

        for (int i = 0; i < laneList.getLength(); i++) {

            Element lane = (Element) laneList.item(i);
            String laneName = lane.getAttribute("name");

            NodeList refs = getElementsByTagName(lane, "bpmn:flowNodeRef");

            for (int j = 0; j < refs.getLength(); j++) {
                String nodeId = refs.item(j).getTextContent();
                WorkflowNode node = findNodeById(graph, nodeId.trim());

                if (node != null && laneName != null && !laneName.isEmpty()) {
                    node.setParticipantName(laneName);
                }
            }
        }
    }

    private NodeList getElementsByTagName(Element element,
                                         String tagName) {

        NodeList list = element.getElementsByTagName(tagName);

        if (list.getLength() > 0) {
            return list;
        }

        String localName = tagName.contains(":")
                ? tagName.substring(tagName.indexOf(":") + 1)
                : tagName;

        return element.getElementsByTagName(localName);
    }
}