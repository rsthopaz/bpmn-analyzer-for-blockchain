package com.blockchain.analyzer.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BPMNTask {

    private String id;

    private String name;

    private String type;

    private String processId;

    private boolean multiParticipant;

    private boolean hasMessageFlow;

    private boolean approvalTask;

    private boolean externalInteraction;

    private boolean dataSensitive;

}