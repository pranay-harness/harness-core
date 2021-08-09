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
