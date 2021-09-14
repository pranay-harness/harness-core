/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.gitsync;

import io.harness.git.model.GitFileChange;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitSyncChangeSet {
  private GitFileChange gitFileChange;
  private String projectIdentifier;
  private String orgIdentifier;
  private String accountId;
}
