package io.harness.ngtriggers.beans.entity.metadata;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitMetadata {
  String connectorIdentifier;
  String repoName;
}
