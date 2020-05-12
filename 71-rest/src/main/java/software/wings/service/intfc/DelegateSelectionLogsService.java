package software.wings.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import software.wings.beans.BatchDelegateSelectionLog;
import software.wings.beans.DelegateScope;

import java.util.List;
import java.util.Set;

public interface DelegateSelectionLogsService {
  void save(BatchDelegateSelectionLog batch);

  BatchDelegateSelectionLog createBatch(DelegateTask task);

  void logCanAssign(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logExcludeScopeMatched(
      BatchDelegateSelectionLog batch, String accountId, String delegateId, DelegateScope scope);

  void logMissingSelector(BatchDelegateSelectionLog batch, String accountId, String delegateId, String selector);

  void logMissingAllSelectors(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logNoIncludeScopeMatched(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  List<DelegateSelectionLogParams> fetchTaskSelectionLogs(String accountId, String taskId);

  void logDisconnectedDelegate(BatchDelegateSelectionLog batch, String accountId, Set<String> delegateIds);

  void logWaitingForApprovalDelegate(BatchDelegateSelectionLog batch, String accountId, Set<String> delegateIds);
}
