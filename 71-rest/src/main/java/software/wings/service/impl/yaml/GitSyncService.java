package software.wings.service.impl.yaml;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.yaml.errorhandling.GitSyncError;

import java.util.List;

public interface GitSyncService {
  /**
   *
   * @param req
   * @return
   */
  PageResponse<GitSyncError> list(PageRequest<GitSyncError> req);

  /**
   *
   * @param accountId
   * @param errors
   */
  void discardGitSyncErrorsForGivenIds(String accountId, List<GitSyncError> errors);
}
