package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GitSyncEntityDTOKeys")
@Schema(name = "GitSyncEntity", description = "This contains details of the Git Sync Entity")
@OwnedBy(DX)
public class GitSyncEntityDTO {
  @Schema(description = "Name of the Entity") private String entityName;
  @Schema(description = GitSyncApiConstants.ENTITY_TYPE_PARAM_MESSAGE) private EntityType entityType;
  @Schema(description = "Id of the Entity") private String entityIdentifier;
  @Schema(description = "Id of the Connector referenced in Git") private String gitConnectorId;
  @Schema(description = GitSyncApiConstants.REPO_URL_PARAM_MESSAGE) @JsonProperty("repoUrl") private String repo;
  @Schema(description = GitSyncApiConstants.BRANCH_PARAM_MESSAGE) private String branch;
  @Schema(description = GitSyncApiConstants.FOLDER_PATH_PARAM_MESSAGE) private String folderPath;
  @Schema(description = GitSyncApiConstants.FILEPATH_PARAM_MESSAGE) private String entityGitPath;
  @Schema(description = "Type of Git Repo Provider") private RepoProviders repoProviderType;
  @Schema(description = "This contains details about the Entity’s Scope and its Identifier")
  private EntityReference entityReference;
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @JsonIgnore String accountId;
}
