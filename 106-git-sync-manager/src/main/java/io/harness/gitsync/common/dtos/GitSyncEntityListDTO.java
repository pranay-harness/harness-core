package io.harness.gitsync.common.dtos;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncEntityListDTO {
  private String entityType;
  private List<GitSyncEntityDTO> gitSyncEntities;
}
