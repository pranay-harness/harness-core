package io.harness.RestUtils;

import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import software.wings.beans.Pipeline;

import javax.ws.rs.core.GenericType;

public class PipelineRestUtils {
  public Pipeline createPipeline(String appId, Pipeline pipeline, String accountId, String bearerToken) {
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

  public Pipeline getPipeline(String appId, String pipelineId, String bearerToken) {
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

  public Pipeline updatePipeline(String appId, Pipeline pipeline, String bearerToken) {
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
}
