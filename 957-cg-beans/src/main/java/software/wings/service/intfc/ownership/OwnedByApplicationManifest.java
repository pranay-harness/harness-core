package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface OwnedByApplicationManifest {
  void pruneByApplicationManifest(String appId, String applicationManifestId);
}
