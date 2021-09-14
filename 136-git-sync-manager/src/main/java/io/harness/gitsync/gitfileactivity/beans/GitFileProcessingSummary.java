/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.gitfileactivity.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@Document("gitFileProcessingSummaryNG")
@TypeAlias("io.harness.gitsync.gitfileactivity.beans.gitFileProcessingSummary")
@OwnedBy(DX)
public class GitFileProcessingSummary {
  private final Long failureCount;
  private final Long successCount;
  private final Long totalCount;
  private final Long skippedCount;
  private final Long queuedCount;
}
