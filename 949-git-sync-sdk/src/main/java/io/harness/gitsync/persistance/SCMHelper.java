package io.harness.gitsync.persistance;

import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.git.model.ChangeType;

import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class SCMHelper<Y, B extends GitSyncableEntity> {
  @SneakyThrows
  String pushToGit(Y yaml, ChangeType changeType, ScmConnector scmConnector, String filePath) {
    log.info("pushed to git");
    return "commitid";
  }
}
