package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GitSyncEntityListDTOKeys")
@Schema(name = "GitSyncEntityList", description = "This contains list of Entities based on Entity Type")
@OwnedBy(DX)
public class GitSyncEntityListDTO {
  @Schema(description = GitSyncApiConstants.ENTITY_TYPE_PARAM_MESSAGE) private EntityType entityType;
  @Schema(description = "This is the number of Git Sync entities corresponding to a given entity type")
  private long count;
  @Schema(description = "This is the list of all the Git Sync entities corresponding to a given entity type")
  private List<GitSyncEntityDTO> gitSyncEntities;
}
