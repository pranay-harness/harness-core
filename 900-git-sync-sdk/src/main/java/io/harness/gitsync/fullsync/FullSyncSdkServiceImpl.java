package io.harness.gitsync.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.FileChanges;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class FullSyncSdkServiceImpl implements FullSyncSdkService {
  Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;

  @Override
  public FileChanges getFileChanges(ScopeDetails scopeDetails) {
    List<FileChange> fileChangeList = new ArrayList<>();

    gitPersistenceHelperServiceMap.forEach((entityType, gitPersistenceHelperService) -> {
      final List<FileChange> entitiesForFullSync = gitPersistenceHelperService.listAllEntities(scopeDetails);
      log.info("Entities for full sync for entityType [{}]: [{}]", entityType, entitiesForFullSync);
      fileChangeList.addAll(entitiesForFullSync);
    });
    return FileChanges.newBuilder().addAllFileChanges(fileChangeList).build();
  }
}
