package io.harness.gitsync.common.service;

import io.harness.beans.NGPageResponse;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncProductDTO;
import io.harness.gitsync.core.EntityType;
import io.harness.gitsync.core.Product;

public interface GitEntityService {
  GitSyncProductDTO list(String projectId, String orgId, String accountId, Product product, int size);

  NGPageResponse<GitSyncEntityListDTO> getPageByType(
      String projectId, String orgId, String accountId, EntityType entityType, int page, int size);
}
