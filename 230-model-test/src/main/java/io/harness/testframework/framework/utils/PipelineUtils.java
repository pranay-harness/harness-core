/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.testframework.restutils.PipelineRestUtils;

import software.wings.beans.Account;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PipelineUtils {
  public static PipelineStage prepareApprovalStage(Account account, String bearerToken, String pipelineId) {
    final String userGroupName = "Account Administrator";
    PipelineStage approvalStage = new PipelineStage();
    approvalStage.setName(TestUtils.generateRandomUUID());
    approvalStage.setParallel(false);
    List<PipelineStageElement> pipelineStageElements = new ArrayList<>();
    PipelineStageElement pipelineStageElement = new PipelineStageElement();
    pipelineStageElement.setName("APPROVAL 0");
    pipelineStageElement.setType(StateType.APPROVAL.getType());
    Map<String, Object> properties = new HashMap<>();
    properties.put("approvalStateType", ApprovalStateType.USER_GROUP.name());
    UserGroup userGroup = UserGroupUtils.getUserGroup(account, bearerToken, userGroupName);
    assertThat(userGroup).isNotNull();
    properties.put("timeoutInMills", 1000 * 60 * 60);
    properties.put("groupName", userGroupName);
    List<String> userGroups = new ArrayList<>();
    if (userGroup != null) {
      userGroups.add(userGroup.getUuid());
    }
    properties.put("userGroups", userGroups);
    pipelineStageElement.setProperties(properties);
    pipelineStageElement.setUuid(TestUtils.generateRandomUUID());
    pipelineStageElements.add(pipelineStageElement);
    approvalStage.setPipelineStageElements(pipelineStageElements);
    return approvalStage;
  }

  public static PipelineStage prepareExecutionStage(String envId, String workflowId, Map<String, String> variables) {
    PipelineStage executionStage = new PipelineStage();
    executionStage.setName(TestUtils.generateRandomUUID());
    executionStage.setParallel(false);
    List<PipelineStageElement> pipelineStageElements = new ArrayList<>();
    PipelineStageElement pipelineStageElement = new PipelineStageElement();
    pipelineStageElement.setName(TestUtils.generateRandomUUID());
    pipelineStageElement.setType(StateType.ENV_STATE.getType());
    pipelineStageElement.setUuid(TestUtils.generateRandomUUID());
    Map<String, Object> properties = new HashMap<>();
    properties.put("envId", envId);
    properties.put("workflowId", workflowId);
    pipelineStageElement.setProperties(properties);
    pipelineStageElement.setWorkflowVariables(variables);
    pipelineStageElements.add(pipelineStageElement);
    executionStage.setPipelineStageElements(pipelineStageElements);
    return executionStage;
  }

  public static PipelineStage createPipelineStageWithWorkflow(
      String name, Workflow workflow, Map<String, String> workflowVariables, boolean parallel) {
    ImmutableMap<String, Object> properties =
        ImmutableMap.<String, Object>of("envId", workflow.getEnvId(), "workflowId", workflow.getUuid());
    return PipelineStage.builder()
        .name(name)
        .parallel(parallel)
        .pipelineStageElements(Arrays.asList(PipelineStageElement.builder()
                                                 .name(name)
                                                 .type(StateType.ENV_STATE.toString())
                                                 .properties(properties)
                                                 .workflowVariables(workflowVariables)
                                                 .build()))
        .build();
  }

  public static Pipeline createApprovalPipeline(
      String pipelineName, Account account, String bearerToken, String appId) {
    Pipeline pipeline = new Pipeline();
    pipeline.setName(pipelineName);
    pipeline.setDescription("description");
    List<PipelineStage> pipelineStages = new ArrayList<>();
    PipelineStage approvalStep = PipelineUtils.prepareApprovalStage(account, bearerToken, generateUuid());
    pipelineStages.add(approvalStep);
    pipeline.setPipelineStages(pipelineStages);
    return PipelineRestUtils.createPipeline(appId, pipeline, account.getUuid(), bearerToken);
  }
}
