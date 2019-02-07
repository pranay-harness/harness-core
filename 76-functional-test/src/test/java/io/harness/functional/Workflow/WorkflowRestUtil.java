package io.harness.functional.Workflow;

import static io.restassured.RestAssured.given;

import io.harness.functional.AbstractFunctionalTest;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.RestResponse;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;

import javax.ws.rs.core.GenericType;

public class WorkflowRestUtil extends AbstractFunctionalTest {
  public static Workflow createWorkflow(String accountId, String appId, Workflow workflow) {
    GenericType<RestResponse<Workflow>> workflowType = new GenericType<RestResponse<Workflow>>() {};

    RestResponse<Workflow> savedWorkflowResponse = given()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("accountId", accountId)
                                                       .queryParam("appId", appId)
                                                       .body(workflow, ObjectMapperType.GSON)
                                                       .contentType(ContentType.JSON)
                                                       .post("/workflows")
                                                       .as(workflowType.getType());

    return savedWorkflowResponse.getResource();
  }

  public static WorkflowExecution runWorkflow(String appId, String envId, ExecutionArgs executionArgs) {
    GenericType<RestResponse<WorkflowExecution>> workflowExecutionType =
        new GenericType<RestResponse<WorkflowExecution>>() {};

    RestResponse<WorkflowExecution> savedWorkflowExecutionResponse = given()
                                                                         .auth()
                                                                         .oauth2(bearerToken)
                                                                         .queryParam("appId", appId)
                                                                         .queryParam("envId", envId)
                                                                         .contentType(ContentType.JSON)
                                                                         .body(executionArgs, ObjectMapperType.GSON)
                                                                         .post("/executions")
                                                                         .as(workflowExecutionType.getType());

    return savedWorkflowExecutionResponse.getResource();
  }
}
