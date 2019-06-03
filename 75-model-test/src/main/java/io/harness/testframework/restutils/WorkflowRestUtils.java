package io.harness.testframework.restutils;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Singleton;

import io.harness.beans.WorkflowType;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;

import javax.ws.rs.core.GenericType;

@Singleton
public class WorkflowRestUtils {
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
      throw new WingsException(String.valueOf(savedWorkflowResponse.getResponseMessages()));
    }

    return savedWorkflowResponse.getResource();
  }

  public static WorkflowExecution startWorkflow(
      String bearerToken, String appId, String envId, ExecutionArgs executionArgs) {
    GenericType<RestResponse<WorkflowExecution>> workflowExecutionType =
        new GenericType<RestResponse<WorkflowExecution>>() {};

    RestResponse<WorkflowExecution> savedWorkflowExecutionResponse = Setup.portal()
                                                                         .auth()
                                                                         .oauth2(bearerToken)
                                                                         .queryParam("appId", appId)
                                                                         .queryParam("envId", envId)
                                                                         .contentType(ContentType.JSON)
                                                                         .body(executionArgs, ObjectMapperType.GSON)
                                                                         .post("/executions")
                                                                         .as(workflowExecutionType.getType());
    if (savedWorkflowExecutionResponse.getResource() == null) {
      throw new WingsException(String.valueOf(savedWorkflowExecutionResponse.getResponseMessages()));
    }

    return savedWorkflowExecutionResponse.getResource();
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
  public static Workflow buildCanaryWorkflowPostDeploymentStep(String name, String envId, GraphNode graphNode) {
    return aWorkflow()
        .name(name)
        .envId(envId)
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).addStep(graphNode).build())
                                   .build())
        .build();
  }
}
