package io.harness.pms.data.progressdata;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsProgressData extends OrchestrationMap {
  public PmsProgressData(Map<String, Object> map) {
    super(map);
  }

  public static PmsProgressData parse(String json) {
    if (json == null) {
      return null;
    }
    return new PmsProgressData(RecastOrchestrationUtils.fromJson(json));
  }

  public static PmsProgressData parse(Map<String, Object> map) {
    if (map == null) {
      return null;
    }

    return new PmsProgressData(map);
  }
}
