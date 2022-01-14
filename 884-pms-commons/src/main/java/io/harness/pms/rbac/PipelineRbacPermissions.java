/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
