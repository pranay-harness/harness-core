package io.harness.delegate.beans.polling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class ManifestPollingResponse implements PollingResponse {
  private List<String> unpublishedVersions;
  private List<String> allVersions;
}
