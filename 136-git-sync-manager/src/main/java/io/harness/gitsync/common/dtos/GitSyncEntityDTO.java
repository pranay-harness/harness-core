package io.harness.gitsync.common.dtos;

import io.harness.gitsync.core.EntityType;
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
  private String repo;
  private String branch;
  private String filePath;
}
