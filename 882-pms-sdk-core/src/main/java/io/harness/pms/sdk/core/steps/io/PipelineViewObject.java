package io.harness.pms.sdk.core.steps.io;

import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

public interface PipelineViewObject {
  default String toViewJson() {
    return RecastOrchestrationUtils.toJson(this);
  }
}
