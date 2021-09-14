/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.git.model;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@SuperBuilder
@ToString(exclude = {"authRequest"})
public class GitBaseRequest {
  private String repoUrl;
  private String branch;
  private String commitId;

  private AuthRequest authRequest;

  private String connectorId;
  @NotEmpty private String accountId;
  private GitRepositoryType repoType;

  public boolean useBranch() {
    return isNotEmpty(branch);
  }

  public boolean useCommitId() {
    return isNotEmpty(commitId);
  }
}
