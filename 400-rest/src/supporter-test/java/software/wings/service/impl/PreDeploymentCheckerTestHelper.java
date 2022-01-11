/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.states.ApprovalState.APPROVAL_STATE_TYPE_VARIABLE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import static java.util.Arrays.asList;

import io.harness.beans.WorkflowType;

import software.wings.WingsBaseTest;
import software.wings.beans.GraphNode;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PreDeploymentCheckerTestHelper extends WingsBaseTest {
  private static final String TEST_ACCOUNT_ID = "ACCOUNT_ID";

  public static final Workflow getWorkflow(boolean addFlowControl) {
    return aWorkflow()
        .name(WORKFLOW_NAME)
        .uuid(generateUuid())
        .appId(APP_ID)
        .accountId(TEST_ACCOUNT_ID)
        .serviceId(SERVICE_ID)
        .infraMappingId(INFRA_MAPPING_ID)
        .workflowType(WorkflowType.ORCHESTRATION)
        .envId(ENV_ID)
        .orchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withPreDeploymentSteps(
                    aPhaseStep(PRE_DEPLOYMENT)
                        .addStep(GraphNode.builder()
                                     .type(addFlowControl ? StateType.BARRIER.name() : StateType.SUB_WORKFLOW.name())
                                     .build())
                        .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                .withWorkflowPhases(Collections.singletonList(
                    WorkflowPhaseBuilder.aWorkflowPhase()
                        .phaseSteps(Collections.singletonList(

                            aPhaseStep(DEPLOY_SERVICE)
                                .addStep(GraphNode.builder()
                                             .type(addFlowControl ? StateType.RESOURCE_CONSTRAINT.name()
                                                                  : StateType.SUB_WORKFLOW.name())
                                             .build()

                                        )
                                .build()

                                ))
                        .build()

                        ))
                .build())
        .build();
  }

  public Pipeline getPipeline() {
    return Pipeline.builder()
        .name("pipeline1")
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .uuid(PIPELINE_ID)
        .pipelineStages(asList(prepareStage()))
        .build();
  }

  private PipelineStage prepareStage() {
    Map<String, Object> envStateproperties = new HashMap<>();
    envStateproperties.put("envId", ENV_ID);
    envStateproperties.put("workflowId", WORKFLOW_ID);

    Map<String, Object> approvalStateProperties = new HashMap<>();
    approvalStateProperties.put(APPROVAL_STATE_TYPE_VARIABLE, ApprovalStateType.SHELL_SCRIPT);

    return PipelineStage.builder()
        .pipelineStageElements(asList(PipelineStageElement.builder()
                                          .name("STAGE1")
                                          .type(StateType.ENV_STATE.name())
                                          .properties(envStateproperties)
                                          .build(),
            PipelineStageElement.builder()
                .name("STAGE2")
                .type(StateType.APPROVAL.name())
                .properties(approvalStateProperties)
                .build()))
        .build();
  }
}
