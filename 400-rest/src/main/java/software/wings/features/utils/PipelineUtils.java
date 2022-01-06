/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features.utils;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.baseline.WorkflowExecutionBaseline.WORKFLOW_ID_KEY;
import static software.wings.features.utils.WorkflowUtils.hasApprovalSteps;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;

import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.features.api.Usage;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PipelineUtils {
  private PipelineUtils() {
    throw new AssertionError();
  }

  private static List<Pipeline> getMatchingPipelines(
      Collection<Pipeline> pipelines, Predicate<PipelineStageElement> predicate) {
    return pipelines.stream().filter(p -> matches(p, predicate)).collect(Collectors.toList());
  }

  public static boolean matches(Pipeline pipeline, Predicate<PipelineStageElement> predicate) {
    return pipeline.getPipelineStages() != null
        && pipeline.getPipelineStages()
               .stream()
               .filter(ps -> isNotEmpty(ps.getPipelineStageElements()))
               .anyMatch(ps -> ps.getPipelineStageElements().stream().anyMatch(predicate));
  }

  public static PageRequest<Pipeline> getPipelinesPageRequest(String accountId) {
    return (PageRequest<Pipeline>) aPageRequest()
        .withLimit(PageRequest.UNLIMITED)
        .addFilter(Pipeline.ACCOUNT_ID_KEY2, EQ, accountId)
        .build();
  }

  public static PageRequest<Workflow> getPipelineWorkflowsPageRequest(Pipeline pipeline) {
    PageRequest<Workflow> workflowPageRequest = null;
    Set<String> workflowIds = getWorkflowIdsOfPipeline(pipeline);
    if (isNotEmpty(workflowIds)) {
      workflowPageRequest = aPageRequest()
                                .addFilter(WorkflowKeys.appId, EQ, pipeline.getAppId())
                                .addFilter(WorkflowKeys.workflowType, Operator.EQ, WorkflowType.ORCHESTRATION)
                                .addFilter(Workflow.ID_KEY2, IN, workflowIds.toArray())
                                .build();
    }
    return workflowPageRequest;
  }

  private static Set<String> getWorkflowIdsOfPipeline(Pipeline pipeline) {
    Set<String> workflowIds = Sets.newHashSet();
    if (isNotEmpty(pipeline.getPipelineStages())) {
      pipeline.getPipelineStages()
          .stream()
          .filter(ps -> isNotEmpty(ps.getPipelineStageElements()))
          .map(PipelineStage::getPipelineStageElements)
          .filter(EmptyPredicate::isNotEmpty)
          .forEach(pses -> pses.forEach(pse -> {
            if (pse != null && isNotEmpty(pse.getProperties()) && pse.getProperties().containsKey(WORKFLOW_ID_KEY)) {
              workflowIds.add((String) pse.getProperties().get(WORKFLOW_ID_KEY));
            }
          }));
    }

    return workflowIds;
  }

  public static Usage toUsage(Pipeline pipeline) {
    return Usage.builder()
        .entityId(pipeline.getUuid())
        .entityName(pipeline.getName())
        .entityType(EntityType.PIPELINE.name())
        .property(PipelineKeys.appId, pipeline.getAppId())
        .build();
  }

  public static List<Pipeline> getPipelinesWithApprovalSteps(
      Collection<Pipeline> pipelines, Set<ApprovalStateType> approvalStepTypes) {
    return getMatchingPipelines(pipelines, pse -> hasApprovalSteps(pse, approvalStepTypes));
  }
}
