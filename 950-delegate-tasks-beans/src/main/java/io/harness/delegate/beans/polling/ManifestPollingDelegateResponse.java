package io.harness.delegate.beans.polling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class ManifestPollingDelegateResponse implements PollingResponseInfc {
  private List<String> unpublishedManifests;
  private Set<String> toBeDeletedKeys;
  boolean firstCollectionOnDelegate;
}
