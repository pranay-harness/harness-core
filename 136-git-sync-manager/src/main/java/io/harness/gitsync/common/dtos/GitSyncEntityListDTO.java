package io.harness.gitsync.common.dtos;

import io.harness.EntityType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncEntityListDTO {
  private EntityType entityType;
  private long count;
  private List<GitSyncEntityDTO> gitSyncEntities;
}
