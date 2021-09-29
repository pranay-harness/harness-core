package software.wings.helpers.ext.jenkins;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public enum BuildMetadataKeys {
  artifactPath,
  url,
  artifactFileName,
  allocationSize,
  fileAttributes,
  path,
  parent
}
