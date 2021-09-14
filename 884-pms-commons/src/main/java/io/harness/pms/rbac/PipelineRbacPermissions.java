/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.rbac;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface PipelineRbacPermissions {
  String PIPELINE_CREATE_AND_EDIT = "core_pipeline_edit";
  String PIPELINE_VIEW = "core_pipeline_view";
  String PIPELINE_DELETE = "core_pipeline_delete";
  String PIPELINE_EXECUTE = "core_pipeline_execute";
}
