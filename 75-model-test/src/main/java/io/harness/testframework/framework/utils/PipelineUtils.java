package io.harness.testframework.framework.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Account;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

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
    pipelineStageElement.setName(TestUtils.generateRandomUUID());
    pipelineStageElement.setType(StateType.APPROVAL.getType());
    Map<String, Object> properties = new HashMap<>();
    properties.put("approvalStateType", ApprovalStateType.USER_GROUP.name());
    UserGroup userGroup = UserGroupUtils.getUserGroup(account, bearerToken, userGroupName);
    assertThat(userGroup).isNotNull();
    properties.put("timeoutInMills", 1000 * 60 * 60);
    properties.put("groupName", userGroupName);
    List<String> userGroups = new ArrayList<>();
    userGroups.add(userGroup.getUuid());
    properties.put("userGroups", userGroups);
    pipelineStageElement.setProperties(properties);
    pipelineStageElement.setUuid(TestUtils.generateRandomUUID());
    pipelineStageElements.add(pipelineStageElement);
    approvalStage.setPipelineStageElements(pipelineStageElements);
    return approvalStage;
  }

  public static PipelineStage prepareExecutionStage(String envId, String workflowId, String pipelineId) {
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
}
