package io.harness.gitsync.common.service;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.beans.NGPageResponse;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncProductDTO;

public interface GitEntityService {
  GitSyncProductDTO list(String projectId, String orgId, String accountId, ModuleType moduleType, int size);

  NGPageResponse<GitSyncEntityListDTO> getPageByType(
      String projectId, String orgId, String accountId, EntityType entityType, int page, int size);
}
