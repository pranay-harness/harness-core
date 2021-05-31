package io.harness.gitsync.common.service;

import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProgress;

import java.util.List;
import org.springframework.data.mongodb.core.query.Update;

public interface GitToHarnessProgressService {
  GitToHarnessProgress save(GitToHarnessProgress gitToHarnessProgress);

  void update(String uuid, Update update);

  void updateFilesInProgressRecord(String uuid, List<GitToHarnessFileProcessingRequest> gitToHarnessFilesToProcess);
}
