package com.blockchain.analyzer.blockchain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssessmentContext {

    private boolean graphHasExternalInteraction;

    private boolean graphHasCrossOrganizationFlow;

    private boolean graphHasExternalDataFlow;

}
