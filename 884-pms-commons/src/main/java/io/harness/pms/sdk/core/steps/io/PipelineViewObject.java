package io.harness.pms.sdk.core.steps.io;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public interface PipelineViewObject {
  default String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(this);
  }
}
