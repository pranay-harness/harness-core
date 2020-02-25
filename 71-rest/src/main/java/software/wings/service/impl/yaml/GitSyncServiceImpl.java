package software.wings.service.impl.yaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;
import software.wings.utils.AlertsUtils;
import software.wings.yaml.errorhandling.GitSyncError;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * git sync service.
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class GitSyncServiceImpl implements GitSyncService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AlertsUtils alertsUtils;

  @Override
  public PageResponse<GitSyncError> list(PageRequest<GitSyncError> req) {
    return wingsPersistence.query(GitSyncError.class, req);
  }

  @Override
  public void discardGitSyncErrorsForGivenIds(String accountId, List<GitSyncError> errors) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.field("_id").in(errors.stream().map(e -> e.getUuid()).collect(Collectors.toList()));
    wingsPersistence.delete(query);
    alertsUtils.closeAlertIfApplicable(accountId, false);
  }
}
