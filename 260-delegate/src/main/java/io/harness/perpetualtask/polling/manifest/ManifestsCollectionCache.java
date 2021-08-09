package io.harness.perpetualtask.polling.manifest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Value;

@Value
@OwnedBy(HarnessTeam.CDC)
public class ManifestsCollectionCache {
  Set<String> allManifestKeys;
  Set<String> unpublishedManifestKeys;

  public ManifestsCollectionCache() {
    this.allManifestKeys = new HashSet<>();
    this.unpublishedManifestKeys = new HashSet<>();
  }

  public boolean needsToPublish() {
    return !unpublishedManifestKeys.isEmpty();
  }

  public void populateCache(List<String> chartVersions) {
    if (isEmpty(chartVersions)) {
      return;
    }

    for (String chartVersion : chartVersions) {
      if (!allManifestKeys.contains(chartVersion)) {
        unpublishedManifestKeys.add(chartVersion);
      }
    }

    allManifestKeys.clear();
    this.allManifestKeys.addAll(new HashSet<>(chartVersions));
  }

  public void clearUnpublishedVersions(List<String> versions) {
    this.unpublishedManifestKeys.removeAll(versions);
  }
}
