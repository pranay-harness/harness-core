package software.wings.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.delegate.beans.DelegateSelectionLogResponse;
import io.harness.selection.log.BatchDelegateSelectionLog;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DelegateSelectionLogsService {
  void save(BatchDelegateSelectionLog batch);

  BatchDelegateSelectionLog createBatch(DelegateTask task);

  void logCanAssign(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logExcludeScopeMatched(BatchDelegateSelectionLog batch, String accountId, String delegateId, String scopeName);

  void logMissingSelector(
      BatchDelegateSelectionLog batch, String accountId, String delegateId, String selector, String selectorOrigin);

  void logMissingAllSelectors(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logTaskAssigned(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logNoIncludeScopeMatched(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logProfileScopeRuleNotMatched(
      BatchDelegateSelectionLog batch, String accountId, String delegateId, String scopingRuleDescription);

  List<DelegateSelectionLogParams> fetchTaskSelectionLogs(String accountId, String taskId);

  DelegateSelectionLogResponse fetchTaskSelectionLogsData(String accountId, String taskId);

  Optional<DelegateSelectionLogParams> fetchSelectedDelegateForTask(String accountId, String taskId);

  void logDisconnectedDelegate(BatchDelegateSelectionLog batch, String accountId, Set<String> delegateIds);

  void logWaitingForApprovalDelegate(BatchDelegateSelectionLog batch, String accountId, Set<String> delegateIds);

  void logDisconnectedScalingGroup(
      BatchDelegateSelectionLog batch, String accountId, Set<String> disconnectedScalingGroup, String groupName);
}
