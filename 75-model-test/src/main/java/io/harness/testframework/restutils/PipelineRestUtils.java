package io.harness.testframework.restutils;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.WorkflowExecution;

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
}
