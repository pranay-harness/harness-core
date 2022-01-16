/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "FullSyncRequest", description = "The full sync request")
@OwnedBy(DX)
public class TriggerFullSyncRequestDTO {
  boolean createPR;
  @NotNull String branch;
  String targetBranchForPR;
  String prTitle;
  @NotNull String yamlGitConfigIdentifier;
  boolean isNewBranch;
  String baseBranch;
  String rootFolder;
}
