package com.blockchain.analyzer.parser;

import com.blockchain.analyzer.model.BPMNTask;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BPMNParser {

    public List<BPMNTask> parse(String filePath) {

        List<BPMNTask> tasks = new ArrayList<>();

        File file = new File(filePath);

        BpmnModelInstance modelInstance =
                Bpmn.readModelFromFile(file);

        Collection<ServiceTask> serviceTasks =
                modelInstance.getModelElementsByType(ServiceTask.class);

        for (ServiceTask task : serviceTasks) {

            BPMNTask bpmnTask = BPMNTask.builder()
                    .id(task.getId())
                    .name(task.getName())
                    .type("serviceTask")
                    .processId(
                            task.getParentElement().getAttributeValue("id")
                    )
                    .multiParticipant(false)
                    .hasMessageFlow(false)
                    .approvalTask(
                            containsApprovalKeyword(task.getName())
                    )
                    .externalInteraction(
                            containsExternalKeyword(task.getName())
                    )
                    .dataSensitive(
                            containsSensitiveKeyword(task.getName())
                    )
                    .build();

            tasks.add(bpmnTask);
        }

        return tasks;
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
                || text.contains("delivery")
                || text.contains("shipment");
    }

    private boolean containsSensitiveKeyword(String text) {

        if (text == null) return false;

        text = text.toLowerCase();

        return text.contains("payment")
                || text.contains("invoice")
                || text.contains("customer");
    }
}