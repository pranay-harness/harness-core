package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GitSyncEntityDTOKeys")
@OwnedBy(DX)
public class GitSyncEntityDTO {
  private String entityName;
  private EntityType entityType;
  private String entityIdentifier;
  private String gitConnectorId;
  private String repoUrl;
  private String branch;
  private String folderPath;
  private String entityGitPath;
  private RepoProviders repoProviderType;
  private EntityReference entityReference;
  @JsonIgnore String accountId;
}
