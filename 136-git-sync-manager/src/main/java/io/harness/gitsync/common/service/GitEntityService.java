package io.harness.gitsync.common.service;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncProductDTO;
import io.harness.ng.beans.PageResponse;

public interface GitEntityService {
  GitSyncProductDTO list(String projectId, String orgId, String accountId, ModuleType moduleType, int size);

  PageResponse<GitSyncEntityListDTO> getPageByType(
      String projectId, String orgId, String accountId, EntityType entityType, int page, int size);
}
