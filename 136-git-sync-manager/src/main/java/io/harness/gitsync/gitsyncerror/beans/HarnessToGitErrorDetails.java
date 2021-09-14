/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.gitsyncerror.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "HarnessToGitErrorDetailsKeys")
@Document("harnessToGitErrorDetailsNG")
@TypeAlias("io.harness.gitsync.gitsyncerror.beans.harnessToGitErrorDetails")
@OwnedBy(PL)
public class HarnessToGitErrorDetails implements GitSyncErrorDetails {
  @Trimmed private String orgIdentifier;
  @Trimmed private String projectIdentifier;
}
