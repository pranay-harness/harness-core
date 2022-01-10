/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 10/16/17.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitCommitResult extends GitCommandResult {
  private String commitId;
  private int commitTime;
  private String commitMessage;

  /**
   * Instantiates a new Git commit result.
   */
  public GitCommitResult() {
    super(GitCommandType.COMMIT);
  }

  /**
   * Instantiates a new Git commit result.
   *
   * @param commitId   the commit id
   * @param commitTime the commit time
   */
  public GitCommitResult(String commitId, int commitTime, String commitMessage) {
    super(GitCommandType.COMMIT);
    this.commitId = commitId;
    this.commitTime = commitTime;
    this.commitMessage = commitMessage;
  }

  /**
   * Sets commit id.
   *
   * @param commitId the commit id
   */
  public void setCommitId(String commitId) {
    this.commitId = commitId;
  }
}
