/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.gitsyncerror.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldNameConstants(innerTypeName = "GitToHarnessErrorDetailsKeys")
@TypeAlias("io.harness.gitsync.gitsyncerror.beans.gitToHarnessErrorDetails")
@OwnedBy(PL)
public class GitToHarnessErrorDetails implements GitSyncErrorDetails {
  @NotEmpty private String gitCommitId;
  private Long commitTime;
  private String yamlContent;
  private String commitMessage;
  @Getter(value = AccessLevel.PRIVATE) @NotEmpty private boolean resolved;
  private String resolvedByCommitId;

  public boolean isResolved() {
    return this.resolved;
  }
}
