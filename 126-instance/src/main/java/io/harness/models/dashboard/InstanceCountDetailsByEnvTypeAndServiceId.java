package io.harness.models.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@OwnedBy(HarnessTeam.DX)
// used to map service id to instance count details map (env type to count)
public class InstanceCountDetailsByEnvTypeAndServiceId {
  private Map<String, InstanceCountDetailsByEnvTypeBase> instanceCountDetailsByEnvTypeBaseMap;
}
