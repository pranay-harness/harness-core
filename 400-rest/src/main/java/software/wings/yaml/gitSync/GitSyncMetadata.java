/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.gitSync;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "GitSyncMetadataKeys")
public class GitSyncMetadata {
  String gitConnectorId;
  private String repositoryName;
  String branchName;
  String yamlGitConfigId;
}
