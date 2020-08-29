package io.harness.gitsync.common.dtos;

import io.harness.gitsync.core.Product;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncProductDTO {
  private Product productName;
  private List<GitSyncEntityListDTO> gitSyncEntityListDTOList;
}
