package io.harness.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public interface ArtifactMetadataKeys {
  String IMAGE = "image";
  String TAG = "tag";
}
