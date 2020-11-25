package io.harness.facilitator.modes;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.Collections;
import java.util.Map;

@OwnedBy(CDC)
public interface ExecutableResponse {
  default Map<String, Object> getMetadata() {
    return Collections.emptyMap();
  }
}
