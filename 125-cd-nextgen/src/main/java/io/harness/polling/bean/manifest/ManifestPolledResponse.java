package io.harness.polling.bean.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PolledResponse;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
public class ManifestPolledResponse implements PolledResponse {
  Set<String> allPolledKeys;
}
