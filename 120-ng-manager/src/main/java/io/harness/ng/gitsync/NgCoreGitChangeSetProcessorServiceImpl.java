package io.harness.ng.gitsync;

import io.harness.ng.core.gitsync.GitChangeProcessorService;
import io.harness.ng.core.gitsync.GitSyncEntities;
import io.harness.ng.core.gitsync.GitSyncEntityTypeComparator;

import java.util.List;

public class NgCoreGitChangeSetProcessorServiceImpl implements GitChangeProcessorService {
  @Override
  public void sort(List<GitSyncEntities> gitSyncChanges) {
    gitSyncChanges.sort(new GitSyncEntityTypeComparator(NgCoreGitProcessingOrder.getEntityProcessingOrder()));
  }
}
