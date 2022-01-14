package io.harness.pms.data.stepdetails;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsStepDetails extends OrchestrationMap {
  public PmsStepDetails(Map<String, Object> map) {
    super(map);
  }

  public static PmsStepDetails parse(String json) {
    if (json == null) {
      return null;
    }
    return new PmsStepDetails(RecastOrchestrationUtils.fromJson(json));
  }

  public static PmsStepDetails parse(Map<String, Object> map) {
    if (map == null) {
      return null;
    }

    return new PmsStepDetails(map);
  }
}
