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
public class GitPushResult extends GitCommandResult {
  public GitPushResult() {
    super(GitCommandType.PUSH);
  }

  public GitPushResult(RefUpdate refUpdate) {
    super(GitCommandType.PUSH);
    this.refUpdate = refUpdate;
  }

  private RefUpdate refUpdate;

  @Data
  @Builder
  public static class RefUpdate {
    private String expectedOldObjectId;
    private final String newObjectId;
    private boolean forceUpdate;
    private String status;
    private String message;
  }
}
