/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.git.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FetchFilesResult extends GitBaseResult {
  private CommitResult commitResult;
  private List<GitFile> files;

  @Builder
  public FetchFilesResult(String accountId, CommitResult commitResult, List<GitFile> files) {
    super(accountId);
    this.commitResult = commitResult;
    this.files = files;
  }
}
