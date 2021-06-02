package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.DONE;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepType.GET_FILES;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProgress;
import io.harness.gitsync.common.beans.GitToHarnessProgress.GitToHarnessProgressKeys;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.repositories.gittoharnessstatus.GitToHarnessProgressRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitToHarnessProgressServiceImpl implements GitToHarnessProgressService {
  GitToHarnessProgressRepository gitToHarnessProgressRepository;

  @Override
  public GitToHarnessProgress save(GitToHarnessProgress gitToHarnessProgress) {
    return gitToHarnessProgressRepository.save(gitToHarnessProgress);
  }

  @Override
  public void update(String uuid, Update update) {
    Criteria criteria = new Criteria().where(GitToHarnessProgressKeys.uuid).is(uuid);
    gitToHarnessProgressRepository.findAndModify(criteria, update);
  }

  @Override
  public void updateFilesInProgressRecord(
      String uuid, List<GitToHarnessFileProcessingRequest> gitToHarnessFilesToProcess) {
    Update update = new Update();
    update.set(GitToHarnessProgressKeys.gitFileChanges, gitToHarnessFilesToProcess);
    update.set(GitToHarnessProgressKeys.stepType, GET_FILES);
    update.set(GitToHarnessProgressKeys.stepStatus, DONE);
    update(uuid, update);
  }
}
