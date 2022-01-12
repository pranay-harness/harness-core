/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
