package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.git.model.ChangeType;

import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class SCMHelper<Y, B extends GitSyncableEntity> {
  @SneakyThrows
  String pushToGit(Y yaml, ChangeType changeType, ScmConnector scmConnector, String filePath) {
    log.info("pushed to git");
    return "commitid";
  }
}
