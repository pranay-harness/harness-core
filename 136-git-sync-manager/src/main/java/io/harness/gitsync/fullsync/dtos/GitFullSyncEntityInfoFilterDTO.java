/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.fullsync.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "GitFullSyncEntityInfoFilterKeys")
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "GitFullSyncEntityInfoFilter", description = "This contains filters for Git Full Sync")
@OwnedBy(PL)
public class GitFullSyncEntityInfoFilterDTO {
  @Schema(description = GitSyncApiConstants.ENTITY_TYPE_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.ENTITY_TYPE)
  EntityType entityType;

  @Schema(description = GitSyncApiConstants.SYNC_STATUS_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.SYNC_STATUS)
  GitFullSyncEntityInfo.SyncStatus syncStatus;
}
