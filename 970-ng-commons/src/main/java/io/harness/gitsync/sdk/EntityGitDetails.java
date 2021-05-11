package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@OwnedBy(DX)
public class EntityGitDetails {
  String objectId;
  String branch;
  String repoIdentifier;
  String rootFolder;
  String filePath;
}
