package io.harness.pms.data.output;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.data.OrchestrationMap;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSweepingOutput extends OrchestrationMap {
  public PmsSweepingOutput(Map<String, Object> map) {
    super(map);
  }

  public static PmsSweepingOutput parse(String json) {
    if (EmptyPredicate.isEmpty(json)) {
      return null;
    }
    return new PmsSweepingOutput(RecastOrchestrationUtils.fromJson(json));
  }

  public static PmsSweepingOutput parse(Map<String, Object> map) {
    if (EmptyPredicate.isEmpty(map)) {
      return null;
    }
    return new PmsSweepingOutput(map);
  }
}
