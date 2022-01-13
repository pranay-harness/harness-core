/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.exception.EmptyRestResponseException;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.GenericType;

@Singleton
public class WorkflowRestUtils {
  static final String SETUP_CONTAINER_CONSTANT = "Setup Container";
  static final String ECS_SERVICE_SETUP_CONSTANT = "ECS Service Setup";
  static final String UPGRADE_CONTAINERS_CONSTANT = "Upgrade Containers";
  static final String DEPLOY_CONTAINERS_CONSTANT = "Deploy Containers";

  public static Workflow getWorkflow(String appId, String workflowId, String bearerToken) {
    GenericType<RestResponse<Workflow>> workflowType = new GenericType<RestResponse<Workflow>>() {};

    RestResponse<Workflow> savedWorkflowResponse = Setup.portal()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("appId", appId)
                                                       .contentType(ContentType.JSON)
                                                       .get("/workflows/" + workflowId)
                                                       .as(workflowType.getType());

    return savedWorkflowResponse.getResource();
  }

  public static Workflow createWorkflow(String bearerToken, String accountId, String appId, Workflow workflow) {
    GenericType<RestResponse<Workflow>> workflowType = new GenericType<RestResponse<Workflow>>() {};

    RestResponse<Workflow> savedWorkflowResponse = Setup.portal()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("accountId", accountId)
                                                       .queryParam("appId", appId)
                                                       .body(workflow, ObjectMapperType.GSON)
                                                       .contentType(ContentType.JSON)
                                                       .post("/workflows")
                                                       .as(workflowType.getType());

    if (savedWorkflowResponse.getResource() == null) {
      throw new InvalidRequestException(String.valueOf(savedWorkflowResponse.getResponseMessages()));
    }

    return savedWorkflowResponse.getResource();
  }

  public static Workflow updateWorkflow(String bearerToken, String accountId, String appId, Workflow workflow) {
    GenericType<RestResponse<Workflow>> workflowType = new GenericType<RestResponse<Workflow>>() {};

    RestResponse<Workflow> updatedWorkflowResponse = Setup.portal()
                                                         .auth()
                                                         .oauth2(bearerToken)
                                                         .queryParam("accountId", accountId)
                                                         .queryParam("appId", appId)
                                                         .body(workflow, ObjectMapperType.GSON)
                                                         .contentType(ContentType.JSON)
                                                         .put("/workflows/" + workflow.getUuid() + "/basic")
                                                         .as(workflowType.getType());

    return updatedWorkflowResponse.getResource();
  }

  public static WorkflowPhase updateWorkflowPhase(String bearerToken, String accountId, String appId, String workflowId,
      String phaseId, WorkflowPhase workflowPhase) {
    GenericType<RestResponse<WorkflowPhase>> returnType = new GenericType<RestResponse<WorkflowPhase>>() {};

    RestResponse<WorkflowPhase> savedWorkflowResponse = Setup.portal()
                                                            .auth()
                                                            .oauth2(bearerToken)
                                                            .queryParam("accountId", accountId)
                                                            .queryParam("appId", appId)
                                                            .body(workflowPhase, ObjectMapperType.GSON)
                                                            .contentType(ContentType.JSON)
                                                            .put("/workflows/" + workflowId + "/phases/" + phaseId)
                                                            .as(returnType.getType());

    return savedWorkflowResponse.getResource();
  }

  public static List<Variable> updateUserVariables(
      String bearerToken, String accountId, String appId, String workflowId, List<Variable> userVariables) {
    GenericType<RestResponse<List<Variable>>> returnType = new GenericType<RestResponse<List<Variable>>>() {};

    RestResponse<List<Variable>> savedWorkflowResponse = Setup.portal()
                                                             .auth()
                                                             .oauth2(bearerToken)
                                                             .queryParam("accountId", accountId)
                                                             .queryParam("appId", appId)
                                                             .body(userVariables, ObjectMapperType.GSON)
                                                             .contentType(ContentType.JSON)
                                                             .put("/workflows/" + workflowId + "/user-variables")
                                                             .as(returnType.getType());

    return savedWorkflowResponse.getResource();
  }

  public static WorkflowExecution startWorkflow(
      String bearerToken, String appId, String envId, ExecutionArgs executionArgs) {
    GenericType<RestResponse<WorkflowExecution>> workflowExecutionType =
        new GenericType<RestResponse<WorkflowExecution>>() {};

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("appId", appId);
    if (envId != null) {
      queryParams.put("envId", envId);
    }

    RestResponse<WorkflowExecution> savedWorkflowExecutionResponse = Setup.portal()
                                                                         .auth()
                                                                         .oauth2(bearerToken)
                                                                         .queryParams(queryParams)
                                                                         .contentType(ContentType.JSON)
                                                                         .body(executionArgs, ObjectMapperType.GSON)
                                                                         .post("/executions")
                                                                         .as(workflowExecutionType.getType());
    if (savedWorkflowExecutionResponse.getResource() == null) {
      throw new InvalidRequestException(String.valueOf(savedWorkflowExecutionResponse.getResponseMessages()));
    }

    return savedWorkflowExecutionResponse.getResource();
  }

  public static WorkflowExecution rollbackExecution(String bearerToken, String appId, String workflowExecutionId) {
    GenericType<RestResponse<WorkflowExecution>> workflowExecutionType =
        new GenericType<RestResponse<WorkflowExecution>>() {};
    RestResponse<WorkflowExecution> rollbackExecutionResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("appId", appId)
            .queryParam("workflowExecutionId", workflowExecutionId)
            .body(new HashMap<>(), ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .post("/executions/triggerRollback")
            .as(workflowExecutionType.getType());
    if (rollbackExecutionResponse.getResource() == null) {
      throw new EmptyRestResponseException(
          "/executions/triggerRollback", String.valueOf(rollbackExecutionResponse.getResponseMessages()));
    }
    return rollbackExecutionResponse.getResource();
  }

  public static WorkflowPhase saveWorkflowPhase(
      String bearerToken, String appId, String workflowId, String phaseId, WorkflowPhase phase) {
    GenericType<RestResponse<WorkflowPhase>> workflowExecutionType = new GenericType<RestResponse<WorkflowPhase>>() {};

    RestResponse<WorkflowPhase> savedWorkflowPhaseResponse = Setup.portal()
                                                                 .auth()
                                                                 .oauth2(bearerToken)
                                                                 .queryParam("appId", appId)
                                                                 .contentType(ContentType.JSON)
                                                                 .body(phase, ObjectMapperType.GSON)
                                                                 .put("/workflows/" + workflowId + "/phases/" + phaseId)
                                                                 .as(workflowExecutionType.getType());

    return savedWorkflowPhaseResponse.getResource();
  }

  public static Object deleteWorkflow(String bearerToken, String workflowId, String appId) {
    GenericType<RestResponse> workflowType = new GenericType<RestResponse>() {};
    RestResponse savedResponse = Setup.portal()
                                     .auth()
                                     .oauth2(bearerToken)
                                     .contentType(ContentType.JSON)
                                     .queryParam("appId", appId)
                                     .queryParam("sort[0][field]", "createdAt")
                                     .queryParam("sort[0][direction]", "DESC")
                                     .delete("/workflows/" + workflowId)
                                     .as(workflowType.getType());

    return savedResponse.getResource();
  }

  public static PhaseStep ecsContainerDeployPhaseStep() {
    return aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS_CONSTANT)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(ECS_SERVICE_DEPLOY.name())
                     .name(UPGRADE_CONTAINERS_CONSTANT)
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("instanceUnitType", "PERCENTAGE")
                                     .put("instanceCount", 100)
                                     .put("downsizeInstanceUnitType", "PERCENTAGE")
                                     .put("downsizeInstanceCount", 0)
                                     .build())
                     .build())
        .build();
  }

  public static PhaseStep ecsContainerSetupPhaseStep() {
    return aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER_CONSTANT)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(ECS_SERVICE_SETUP.name())
                     .name(ECS_SERVICE_SETUP_CONSTANT)
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("fixedInstances", "1")
                                     .put("useLoadBalancer", false)
                                     .put("ecsServiceName", "${app.name}__${service.name}__BASIC")
                                     .put("desiredInstanceCount", "fixedInstances")
                                     .put("resizeStrategy", ResizeStrategy.DOWNSIZE_OLD_FIRST)
                                     .put("serviceSteadyStateTimeout", 10)
                                     .build())
                     .build())
        .build();
  }
}
