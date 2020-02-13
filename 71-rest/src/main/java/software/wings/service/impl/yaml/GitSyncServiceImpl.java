package software.wings.service.impl.yaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.errorhandling.GitSyncError;

import javax.validation.executable.ValidateOnExecution;

/**
 * git sync service.
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class GitSyncServiceImpl implements GitSyncService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<GitSyncError> list(PageRequest<GitSyncError> req) {
    return wingsPersistence.query(GitSyncError.class, req);
  }
}
