/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.git.model;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CommitAndPushRequest extends GitBaseRequest {
  private String lastProcessedGitCommit;
  private boolean pushOnlyIfHeadSeen;
  private List<GitFileChange> gitFileChanges;
  private boolean forcePush;
  private String commitMessage;
  private String authorName;
  private String authorEmail;
}
