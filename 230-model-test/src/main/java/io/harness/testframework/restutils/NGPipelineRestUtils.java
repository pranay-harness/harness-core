/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PIPELINE_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;

import io.harness.ng.core.dto.ResponseDTO;
import io.harness.testframework.framework.Setup;

import java.util.Map;
import javax.ws.rs.core.GenericType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGPipelineRestUtils {
  public static final String PIPELINE_KEY_PARAM = "{" + PIPELINE_KEY + "}";
  public static final String BASE_PATH = "pipelines";

  public static String createPipeline(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineYaml) {
    ResponseDTO<String> pipelineResponse = Setup.ci()
                                               .log()
                                               .method()
                                               .log()
                                               .uri()
                                               .basePath(BASE_PATH)
                                               .queryParam(ACCOUNT_KEY, accountIdentifier)
                                               .queryParam(ORG_KEY, orgIdentifier)
                                               .queryParam(PROJECT_KEY, projectIdentifier)
                                               .body(pipelineYaml)
                                               .post()
                                               .as(new GenericType<ResponseDTO<String>>() {}.getType());
    return pipelineResponse.getData();
  }

  public static Map<String, Object> readPipeline(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineName) {
    ResponseDTO<Map<String, Object>> pipelineResponse =
        Setup.ci()
            .log()
            .method()
            .log()
            .uri()
            .basePath(BASE_PATH)
            .queryParam(ACCOUNT_KEY, accountIdentifier)
            .queryParam(ORG_KEY, orgIdentifier)
            .queryParam(PROJECT_KEY, projectIdentifier)
            .pathParams(PIPELINE_KEY, pipelineName)
            .get(PIPELINE_KEY_PARAM)
            .as(new GenericType<ResponseDTO<Map<String, Object>>>() {}.getType());
    return pipelineResponse.getData();
  }

  public static String updatePipeline(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineName, String pipelineYaml) {
    ResponseDTO<String> pipelineResponse = Setup.ci()
                                               .log()
                                               .method()
                                               .log()
                                               .uri()
                                               .basePath(BASE_PATH)
                                               .queryParam(ACCOUNT_KEY, accountIdentifier)
                                               .queryParam(ORG_KEY, orgIdentifier)
                                               .queryParam(PROJECT_KEY, projectIdentifier)
                                               .pathParams(PIPELINE_KEY, pipelineName)
                                               .body(pipelineYaml)
                                               .put(PIPELINE_KEY_PARAM)
                                               .as(new GenericType<ResponseDTO<String>>() {}.getType());
    return pipelineResponse.getData();
  }

  public static Boolean deletePipeline(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineName) {
    ResponseDTO<Boolean> pipelineResponse = Setup.ci()
                                                .log()
                                                .method()
                                                .log()
                                                .uri()
                                                .basePath(BASE_PATH)
                                                .queryParam(ACCOUNT_KEY, accountIdentifier)
                                                .queryParam(ORG_KEY, orgIdentifier)
                                                .queryParam(PROJECT_KEY, projectIdentifier)
                                                .pathParams(PIPELINE_KEY, pipelineName)
                                                .delete(PIPELINE_KEY_PARAM)
                                                .as(new GenericType<ResponseDTO<Boolean>>() {}.getType());
    return pipelineResponse.getData();
  }
}
