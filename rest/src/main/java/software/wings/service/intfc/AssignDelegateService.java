package software.wings.service.intfc;

import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;

/**
 * Created by brett on 7/20/17
 */
public interface AssignDelegateService {
  boolean canAssign(DelegateTask task, String delegateId);

  boolean isWhitelisted(DelegateTask task, String delegateId);

  void saveConnectionResults(List<DelegateConnectionResult> results);

  void clearConnectionResults(String delegateId);
}
