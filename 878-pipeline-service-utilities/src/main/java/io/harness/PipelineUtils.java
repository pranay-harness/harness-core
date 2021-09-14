/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import io.harness.ng.core.NGAccess;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineUtils {
  public String getBuildDetailsUrl(NGAccess ngAccess, String pipelineId, String executionId, String ngBaseUrl) {
    String detailsUrl = new StringBuilder(ngBaseUrl)
                            .append(String.format("/account/%s", ngAccess.getAccountIdentifier()))
                            .append(String.format("/ci/orgs/%s", ngAccess.getOrgIdentifier()))
                            .append(String.format("/projects/%s", ngAccess.getProjectIdentifier()))
                            .append(String.format("/pipelines/%s", pipelineId))
                            .append(String.format("/executions/%s", executionId))
                            .append("/pipeline")
                            .toString();
    log.info("DetailsUrl is: [{}]", detailsUrl);
    return detailsUrl;
  }
}
