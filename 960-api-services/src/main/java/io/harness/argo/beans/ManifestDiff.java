package io.harness.argo.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ManifestDiff {
  String resourceIdentifier;
  String clusterManifest;
  String gitManifest;
}
