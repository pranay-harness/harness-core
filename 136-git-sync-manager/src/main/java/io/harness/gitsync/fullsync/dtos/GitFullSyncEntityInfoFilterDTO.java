package io.harness.gitsync.fullsync.dtos;

import io.harness.EntityType;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.Parameter;
import javax.ws.rs.QueryParam;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitFullSyncEntityInfoFilterDTO {
  @Parameter(description = GitSyncApiConstants.ENTITY_TYPE_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.ENTITY_TYPE)
  EntityType entityType;

  @Parameter(description = GitSyncApiConstants.SYNC_STATUS_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.SYNC_STATUS)
  GitFullSyncEntityInfo.SyncStatus syncStatus;
}
