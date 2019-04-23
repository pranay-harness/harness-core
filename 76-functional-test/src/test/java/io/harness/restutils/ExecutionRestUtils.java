package io.harness.restutils;

import com.google.inject.Singleton;

import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import software.wings.beans.Account;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.WorkflowExecution;

import java.util.Map;
import javax.ws.rs.core.GenericType;

@Singleton
public class ExecutionRestUtils {
  /**
   *
   * @param appId
   * @param envId
   * @param executionArgs
   * @return Workflow execution status
   */
  public static WorkflowExecution runWorkflow(
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
    return savedWorkflowExecutionResponse.getResource();
  }

  public static Map<String, Object> runPipeline(
      String bearerToken, String appId, String envId, String pipelineId, ExecutionArgs executionArgs) {
    GenericType<RestResponse<Map<String, Object>>> pipelineExecutionType =
        new GenericType<RestResponse<Map<String, Object>>>() {};

    RestResponse<Map<String, Object>> savedWorkflowExecutionResponse = Setup.portal()
                                                                           .auth()
                                                                           .oauth2(bearerToken)
                                                                           .queryParam("appId", appId)
                                                                           .queryParam("envId", envId)
                                                                           .queryParam("pipelineId", pipelineId)
                                                                           .contentType(ContentType.JSON)
                                                                           .body(executionArgs, ObjectMapperType.GSON)
                                                                           .post("/executions")
                                                                           .as(pipelineExecutionType.getType());

    return savedWorkflowExecutionResponse.getResource();
  }

  /**
   *
   * @param appId
   * @param executionId
   * @return returns execution status
   */
  public static String getExecutionStatus(String bearerToken, Account account, String appId, String executionId) {
    int i = 0;
    String status = "FAILED";
    while (i < 60) {
      JsonPath jsonPath = Setup.portal()
                              .auth()
                              .oauth2(bearerToken)
                              .queryParam("appId", appId)
                              .queryParam("accountId", account.getUuid())
                              .contentType(ContentType.JSON)
                              .get("/executions/" + executionId)
                              .getBody()
                              .jsonPath();
      Map<Object, Object> resource = jsonPath.getMap("resource");
      status = resource.get("status").toString();
      System.out.println(status);
      if (status.equals("SUCCESS") || status.equals("FAILED")) {
        return status;
      }
      try {
        Thread.sleep(10000);
        i++;
        System.out.println(i);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return status;
  }

  public static String getWorkflowExecutionStatus(
      String bearerToken, Account account, String appId, String executionId) {
    JsonPath jsonPath = Setup.portal()
                            .auth()
                            .oauth2(bearerToken)
                            .queryParam("appId", appId)
                            .queryParam("accountId", account.getUuid())
                            .contentType(ContentType.JSON)
                            .get("/executions/" + executionId)
                            .getBody()
                            .jsonPath();
    Map<Object, Object> resource = jsonPath.getMap("resource");
    String status = resource.get("status").toString();

    return status;
  }
}