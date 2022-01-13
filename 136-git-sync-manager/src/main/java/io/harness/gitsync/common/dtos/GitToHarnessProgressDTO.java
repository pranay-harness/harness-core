/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponseDTO;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepType;
import io.harness.gitsync.common.beans.GitToHarnessProgressStatus;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitToHarnessProgressDTO {
  @NotNull private String uuid;
  @NotNull private String accountIdentifier;
  @NotNull private String yamlChangeSetId;
  @NotNull private String repoUrl;
  @NotNull private String branch;
  @NotNull private String commitId;
  @NotNull private YamlChangeSetEventType eventType;
  @NotNull private GitToHarnessProcessingStepType stepType;
  @NotNull private GitToHarnessProcessingStepStatus stepStatus;
  @NotNull private Long stepStartingTime;
  @NotNull private GitToHarnessProgressStatus gitToHarnessProgressStatus;
  @NotNull private Long lastUpdatedAt;
  List<GitToHarnessFileProcessingRequest> gitFileChanges;
  List<GitToHarnessProcessingResponseDTO> processingResponse;
}
