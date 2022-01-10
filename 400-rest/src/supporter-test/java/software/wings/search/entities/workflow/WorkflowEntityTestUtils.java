/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.entities.workflow;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;

import io.harness.beans.WorkflowType;
import io.harness.data.structure.UUIDGenerator;
import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeEvent.ChangeEventBuilder;
import io.harness.mongo.changestreams.ChangeType;

import software.wings.api.DeploymentType;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Service;
import software.wings.beans.Workflow;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.Arrays;

public class WorkflowEntityTestUtils {
  public static Workflow createWorkflow(String accountId, String appId, String workflowId, String envId,
      String serviceId, Service service, String workflowName) {
    Workflow workflow =
        aWorkflow()
            .name(workflowName)
            .appId(appId)
            .envId(envId)
            .serviceId(serviceId)
            .services(Arrays.asList(service))
            .uuid(workflowId)
            .accountId(accountId)
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                       .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT).build())
                                       .addWorkflowPhase(aWorkflowPhase()
                                                             .name("Phase 1")
                                                             .serviceId(serviceId)
                                                             .deploymentType(DeploymentType.SSH)
                                                             .infraMappingId(UUIDGenerator.generateUuid())
                                                             .build())
                                       .withPostDeploymentSteps(aPhaseStep(PhaseStepType.POST_DEPLOYMENT).build())
                                       .build())
            .build();

    workflow.setOrchestration(aCanaryOrchestrationWorkflow()
                                  .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT).build())
                                  .addWorkflowPhase(aWorkflowPhase()
                                                        .name("Phase 1")
                                                        .serviceId(serviceId)
                                                        .deploymentType(DeploymentType.SSH)
                                                        .infraMappingId(UUIDGenerator.generateUuid())
                                                        .build())
                                  .withPostDeploymentSteps(aPhaseStep(PhaseStepType.POST_DEPLOYMENT).build())
                                  .build());
    return workflow;
  }

  private static DBObject getWorkflowChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("name", "edited_name");
    basicDBObject.put("appId", "appId");
    basicDBObject.put("envId", "envId");
    basicDBObject.put("orchestration", "orchestration");
    return basicDBObject;
  }

  public static ChangeEvent createWorkflowChangeEvent(Workflow workflow, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(workflow)
                             .token("token")
                             .uuid(workflow.getUuid())
                             .entityType(Workflow.class);

    if (changeType == ChangeType.UPDATE) {
      changeEventBuilder = changeEventBuilder.changes(getWorkflowChanges());
    }

    return changeEventBuilder.build();
  }
}
