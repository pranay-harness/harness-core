/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.delegate.beans.DelegateSelectionLogResponse;
import io.harness.selection.log.BatchDelegateSelectionLog;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@OwnedBy(DEL)
public interface DelegateSelectionLogsService {
  void save(BatchDelegateSelectionLog batch);

  BatchDelegateSelectionLog createBatch(DelegateTask task);

  void logExcludeScopeMatched(BatchDelegateSelectionLog batch, String accountId, String delegateId, String scopeName);

  void logOwnerRuleNotMatched(
      BatchDelegateSelectionLog batch, String accountId, Set<String> delegateIds, DelegateEntityOwner owner);

  void logMissingSelector(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logTaskAssigned(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logNoIncludeScopeMatched(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logProfileScopeRuleNotMatched(BatchDelegateSelectionLog batch, String accountId, String delegateId,
      String delegateProfileId, Set<String> scopingRulesDescriptions);

  List<DelegateSelectionLogParams> fetchTaskSelectionLogs(String accountId, String taskId);

  DelegateSelectionLogResponse fetchTaskSelectionLogsData(String accountId, String taskId);

  Optional<DelegateSelectionLogParams> fetchSelectedDelegateForTask(String accountId, String taskId);

  void logDisconnectedDelegate(BatchDelegateSelectionLog batch, String accountId, Set<String> delegateIds);

  void logDisconnectedScalingGroup(
      BatchDelegateSelectionLog batch, String accountId, Set<String> disconnectedScalingGroup, String groupName);

  void logMustExecuteOnDelegateMatched(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logMustExecuteOnDelegateNotMatched(BatchDelegateSelectionLog batch, String accountId, String delegateId);

  void logNoEligibleDelegatesToExecuteTask(BatchDelegateSelectionLog batch, String accountId);

  void logNoEligibleDelegatesAvailableToExecuteTask(BatchDelegateSelectionLog batch, String accountId);

  void logEligibleDelegatesToExecuteTask(BatchDelegateSelectionLog batch, Set<String> delegateIds, String accountId);

  void logBroadcastToDelegate(BatchDelegateSelectionLog batch, Set<String> delegateIds, String accountId);
}
