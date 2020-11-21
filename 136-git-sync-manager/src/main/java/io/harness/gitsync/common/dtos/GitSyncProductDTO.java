package io.harness.gitsync.common.dtos;

import io.harness.ModuleType;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncProductDTO {
  private ModuleType moduleType;
  private List<GitSyncEntityListDTO> gitSyncEntityListDTOList;
}
