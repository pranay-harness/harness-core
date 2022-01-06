/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStageGroupedInfo;
import software.wings.beans.WorkflowExecution;

import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.util.List;
import javax.ws.rs.core.GenericType;

public class PipelineRestUtils {
  public static Pipeline createPipeline(String appId, Pipeline pipeline, String accountId, String bearerToken) {
    GenericType<RestResponse<Pipeline>> pipelineType = new GenericType<RestResponse<Pipeline>>() {};

    RestResponse<Pipeline> savedServiceResponse = Setup.portal()
                                                      .auth()
                                                      .oauth2(bearerToken)
                                                      .queryParam("accountId", accountId)
                                                      .queryParam("appId", appId)
                                                      .body(pipeline, ObjectMapperType.GSON)
                                                      .contentType(ContentType.JSON)
                                                      .post("/pipelines")
                                                      .as(pipelineType.getType());

    return savedServiceResponse.getResource();
  }

  public static Pipeline getPipeline(String appId, String pipelineId, String bearerToken) {
    GenericType<RestResponse<Pipeline>> pipelineType = new GenericType<RestResponse<Pipeline>>() {};

    RestResponse<Pipeline> savedServiceResponse = Setup.portal()
                                                      .auth()
                                                      .oauth2(bearerToken)
                                                      .queryParam("appId", appId)
                                                      .queryParam("withServices", false)
                                                      .contentType(ContentType.JSON)
                                                      .get("/pipelines/" + pipelineId)
                                                      .as(pipelineType.getType());

    return savedServiceResponse.getResource();
  }

  public static List<PipelineStageGroupedInfo> getResumeStages(
      String appId, String accountId, String workflowExecutionId, String bearerToken) {
    GenericType<RestResponse<List<PipelineStageGroupedInfo>>> pipelineType =
        new GenericType<RestResponse<List<PipelineStageGroupedInfo>>>() {};

    RestResponse<List<PipelineStageGroupedInfo>> savedServiceResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("appId", appId)
            .queryParam("accountId", accountId)
            .queryParam("workflowExecutionId", workflowExecutionId)
            .contentType(ContentType.JSON)
            .get("/executions/resumeStages")
            .as(pipelineType.getType());

    return savedServiceResponse.getResource();
  }

  public static Pipeline updatePipeline(String appId, Pipeline pipeline, String bearerToken) {
    GenericType<RestResponse<Pipeline>> pipelineType = new GenericType<RestResponse<Pipeline>>() {};

    RestResponse<Pipeline> savedPipelineResponse = Setup.portal()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("appId", appId)
                                                       .body(pipeline, ObjectMapperType.GSON)
                                                       .contentType(ContentType.JSON)
                                                       .put("/pipelines/" + pipeline.getUuid())
                                                       .as(pipelineType.getType());

    return savedPipelineResponse.getResource();
  }

  public static WorkflowExecution startPipeline(
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

  public static WorkflowExecution resumePipeline(
      String bearerToken, String appId, String accountId, String workflowExecutionId, int parallelIndexToResume) {
    GenericType<RestResponse<WorkflowExecution>> workflowExecutionType =
        new GenericType<RestResponse<WorkflowExecution>>() {};

    RestResponse<WorkflowExecution> savedWorkflowExecutionResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("appId", appId)
            .queryParam("accountId", accountId)
            .queryParam("workflowExecutionId", workflowExecutionId)
            .queryParam("parallelIndexToResume", parallelIndexToResume)
            .contentType(ContentType.JSON)
            .post("/executions/triggerResume")
            .as(workflowExecutionType.getType());

    return savedWorkflowExecutionResponse.getResource();
  }

  public static int deletePipeline(String appId, String pipelineId, String bearerToken) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", appId)
        .contentType(ContentType.JSON)
        .delete("/pipelines/" + pipelineId)
        .statusCode();
  }
}
