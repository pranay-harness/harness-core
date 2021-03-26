package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncProductDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.EntityDetail;

@OwnedBy(DX)
public interface GitEntityService {
  GitSyncProductDTO list(String projectId, String orgId, String accountId, ModuleType moduleType, int size);

  PageResponse<GitSyncEntityListDTO> getPageByType(
      String projectId, String orgId, String accountId, EntityType entityType, int page, int size);

  GitSyncEntityDTO get(EntityReference entityReference, EntityType entityType);

  boolean save(
      String accountId, EntityDetail entityDetail, YamlGitConfigDTO yamlGitConfig, String filePath, String commitId);
}
