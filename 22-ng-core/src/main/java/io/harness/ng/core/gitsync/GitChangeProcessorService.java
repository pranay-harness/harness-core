package io.harness.ng.core.gitsync;

import java.util.List;

public interface GitChangeProcessorService {
  void sort(List<GitSyncEntities> gitSyncChanges);
}
