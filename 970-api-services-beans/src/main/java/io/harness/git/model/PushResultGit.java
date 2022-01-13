/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.git.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PushResultGit extends GitBaseResult {
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

  @Builder(builderMethodName = "pushResultBuilder")
  public PushResultGit(String accountId, RefUpdate refUpdate) {
    super(accountId);
    this.refUpdate = refUpdate;
  }
}
