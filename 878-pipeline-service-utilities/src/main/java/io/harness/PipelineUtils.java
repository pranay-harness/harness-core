package io.harness;

import io.harness.ng.core.NGAccess;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineUtils {
  public String getBuildDetailsUrl(NGAccess ngAccess, String pipelineId, String executionId, String ngBaseUrl) {
    StringBuilder detailsUrl = new StringBuilder(ngBaseUrl);
    detailsUrl.append(String.format("/account/%s", ngAccess.getAccountIdentifier()));
    detailsUrl.append(String.format("/ci/orgs/%s", ngAccess.getOrgIdentifier()));
    detailsUrl.append(String.format("/projects/%s", ngAccess.getProjectIdentifier()));
    detailsUrl.append(String.format("/pipelines/%s", pipelineId));
    detailsUrl.append(String.format("/executions/%s", executionId));
    detailsUrl.append("/pipeline");
    log.info("DetailsUrl is: [{}]", detailsUrl.toString());
    return detailsUrl.toString();
  }
}
