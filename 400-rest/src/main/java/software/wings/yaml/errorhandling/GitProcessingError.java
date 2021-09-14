/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.errorhandling;

import software.wings.beans.GitRepositoryInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitProcessingError {
  private String accountId;
  private String message;
  private Long createdAt;
  private String gitConnectorId;
  private String branchName;
  private String repositoryName;
  private String connectorName;
  private GitRepositoryInfo repositoryInfo;
}
