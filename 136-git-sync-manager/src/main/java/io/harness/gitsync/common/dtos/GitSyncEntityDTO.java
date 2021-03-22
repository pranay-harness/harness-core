package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class GitSyncEntityDTO {
  private String entityName;
  private EntityType entityType;
  private String entityIdentifier;
  private String gitConnectorId;
  @JsonProperty("repositoryName") private String repo;
  private String branch;
  private String filePath;
  private RepoProviders repoProviderType;
}
