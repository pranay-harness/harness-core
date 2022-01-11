/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.Account;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.NameValuePair;
import software.wings.beans.WorkflowExecution;

import com.google.common.collect.ImmutableMap;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;

@Slf4j
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
      log.info(status);
      if (status.equals("SUCCESS") || status.equals("FAILED")) {
        return status;
      }
      try {
        Thread.sleep(10000);
        i++;
        log.info(String.valueOf(i));
      } catch (InterruptedException e) {
        log.error("Error thrown : ", e);
      }
    }
    return status;
  }

  private static String getWorkflowExecutionStatus(
      String bearerToken, Account account, String appId, String executionId) {
    Map<Object, Object> resource = getWorkflowExecution(bearerToken, account, appId, executionId);
    return resource.get("status").toString();
  }

  public static Map<Object, Object> getWorkflowExecution(
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
    return jsonPath.getMap("resource");
  }

  public static void executeAndCheck(
      String bearerToken, Account account, String appId, String envId, ExecutionArgs executionArgs) {
    WorkflowExecution workflowExecution = ExecutionRestUtils.runWorkflow(bearerToken, appId, envId, executionArgs);
    String status =
        ExecutionRestUtils.getWorkflowExecutionStatus(bearerToken, account, appId, workflowExecution.getUuid());
    if (!(status.equals("RUNNING") || status.equals("QUEUED"))) {
      Assert.fail("ERROR: Execution did not START");
    }
  }

  public static void approvePipeline(String bearerToken, Account account, String appId, String executionId,
      String approvalId, ApprovalDetails.Action action, List<NameValuePair> variables) {
    GenericType<RestResponse<Boolean>> approvalResponseType = new GenericType<RestResponse<Boolean>>() {};

    Map<String, Object> body =
        ImmutableMap.of("approvalId", approvalId, "action", action.name(), "variables", variables, "comments", "");
    RestResponse<Boolean> approvalResponse = Setup.portal()
                                                 .auth()
                                                 .oauth2(bearerToken)
                                                 .queryParam("appId", appId)
                                                 .queryParam("accountId", account.getUuid())
                                                 .contentType(ContentType.JSON)
                                                 .body(body, ObjectMapperType.GSON)
                                                 .put("/executions/" + executionId + "/approval")
                                                 .as(approvalResponseType.getType());

    assertThat(approvalResponse.getResource()).withFailMessage(approvalResponse.toString()).isNotNull();
  }
}
