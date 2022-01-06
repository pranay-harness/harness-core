/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.stepstatus;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.stepstatus.artifact.ArtifactMetadata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CI)
public class StepStatus {
  private int numberOfRetries;
  private long totalTimeTakenInMillis;
  private StepExecutionStatus stepExecutionStatus;
  private ArtifactMetadata artifactMetadata;
  @Builder.Default private StepOutput output = StepMapOutput.builder().build();
  @Builder.Default private String error = "";
}
