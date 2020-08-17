package io.harness.gitsync.common.dtos;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncEntityDTO {
  private String entityName;
  private String entityType;
  private String entityIdentifier;
  private String gitConnectorId;
  private String repo;
  private String branch;
  private String yamlPath;
  private String rootPath;
}
