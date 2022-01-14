package io.harness.gitsync.fullsync.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.QueryParam;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
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
