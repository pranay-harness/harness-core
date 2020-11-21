package io.harness.gitsync.common.dtos;

import io.harness.EntityType;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncEntityListDTO {
  private EntityType entityType;
  private long count;
  private List<GitSyncEntityDTO> gitSyncEntities;
}
