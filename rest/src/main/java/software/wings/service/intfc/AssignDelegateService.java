package software.wings.service.intfc;

import software.wings.beans.DelegateTask;
import software.wings.beans.TaskGroup;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;

/**
 * Created by brett on 7/20/17
 */
public interface AssignDelegateService {
  boolean canAssign(String delegateId, DelegateTask task);

  boolean canAssign(String delegateId, String accountId, String appId, String envId, String infraMappingId,
      TaskGroup taskGroup, List<String> tags);

  boolean isWhitelisted(DelegateTask task, String delegateId);

  boolean shouldValidate(DelegateTask task, String delegateId);

  List<String> connectedWhitelistedDelegates(DelegateTask task);

  void saveConnectionResults(List<DelegateConnectionResult> results);

  void clearConnectionResults(String delegateId);
}
