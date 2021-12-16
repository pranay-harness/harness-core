package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class PipelineCRUDErrorResponse {
  public String errorMessageForPipelineNotFound(String orgId, String projectId, String pipelineId) {
    return format("Pipeline [%s] under Project[%s], Organization [%s] doesn't exist or has been deleted.", pipelineId,
        projectId, orgId);
  }
}
