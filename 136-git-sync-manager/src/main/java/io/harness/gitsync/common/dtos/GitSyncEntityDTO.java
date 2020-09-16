package io.harness.gitsync.common.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.EntityType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
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
