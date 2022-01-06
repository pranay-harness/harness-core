/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.ApprovalState.ApprovalStateKeys;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class WorkflowAndPipelineMigrationUtils {
  private WorkflowAndPipelineMigrationUtils() {}

  public static List<Workflow> fetchAllWorkflowsForAccount(
      WingsPersistence wingsPersistence, WorkflowService workflowService, String accountId) {
    long workflowsSize = wingsPersistence.createQuery(Workflow.class).filter(WorkflowKeys.accountId, accountId).count();
    log.info("Total workflows for account = " + workflowsSize);

    int numberOfPages = (int) ((workflowsSize + 999) / 1000);
    List<Workflow> workflows = new ArrayList<>();
    for (int i = 0; i < numberOfPages; i++) {
      List<Workflow> newWorkflows = workflowService
                                        .listWorkflows(aPageRequest()
                                                           .withLimit(UNLIMITED)
                                                           .withOffset(String.valueOf(i * 1000))
                                                           .addFilter(WorkflowKeys.accountId, EQ, accountId)
                                                           .build())
                                        .getResponse();
      if (!isEmpty(newWorkflows)) {
        workflows.addAll(newWorkflows);
      }
    }
    return workflows;
  }

  public static List<Pipeline> fetchAllPipelinesForAccount(
      WingsPersistence wingsPersistence, PipelineService pipelineService, String accountId) {
    long pipelineSize = wingsPersistence.createQuery(Pipeline.class).filter(PipelineKeys.accountId, accountId).count();
    log.info("Total pipelines for account = " + pipelineSize);

    int numberOfPages = (int) ((pipelineSize + 999) / 1000);
    List<Pipeline> pipelines = new ArrayList<>();
    for (int i = 0; i < numberOfPages; i++) {
      List<Pipeline> newPipelines = pipelineService
                                        .listPipelines(aPageRequest()
                                                           .withLimit(UNLIMITED)
                                                           .withOffset(String.valueOf(i * 1000))
                                                           .addFilter(PipelineKeys.accountId, EQ, accountId)
                                                           .build(),
                                            true, 0, false, null)
                                        .getResponse();
      if (!isEmpty(newPipelines)) {
        pipelines.addAll(newPipelines);
      }
    }

    log.info("Updating {} pipelines.", pipelines.size());
    return pipelines;
  }

  public static boolean updateServiceNowProperties(Map<String, Object> properties) {
    if (properties != null && properties.containsKey(ApprovalStateKeys.approvalStateParams)
        && ApprovalStateType.SERVICENOW.name().equals(properties.get(ApprovalState.APPROVAL_STATE_TYPE_VARIABLE))) {
      Map<String, Object> approvalStateParams =
          (Map<String, Object>) properties.get(ApprovalStateKeys.approvalStateParams);
      Map<String, Object> serviceNowApprovalParams =
          (Map<String, Object>) approvalStateParams.get("serviceNowApprovalParams");
      boolean modified = false;
      if (serviceNowApprovalParams.containsKey("approvalValue") && !serviceNowApprovalParams.containsKey("approval")) {
        serviceNowApprovalParams.put("approval",
            ImmutableMap.of("operator", "AND", "conditions",
                Collections.singletonMap("state", Arrays.asList(serviceNowApprovalParams.get("approvalValue")))));
        modified = true;
      }
      if (serviceNowApprovalParams.containsKey("rejectionValue")
          && !serviceNowApprovalParams.containsKey("rejection")) {
        serviceNowApprovalParams.put("rejection",
            ImmutableMap.of("operator", "AND", "conditions",
                Collections.singletonMap("state", Arrays.asList(serviceNowApprovalParams.get("rejectionValue")))));
        modified = true;
      }
      properties.put("approvalStateParams", approvalStateParams);
      return modified;
    }
    return false;
  }
}
