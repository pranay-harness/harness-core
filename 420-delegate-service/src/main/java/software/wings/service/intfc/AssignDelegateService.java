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
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.TaskFailureReason;
import io.harness.selection.log.BatchDelegateSelectionLog;

import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;
import java.util.Map;

@OwnedBy(DEL)
public interface AssignDelegateService {
  boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, DelegateTask task);

  boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, String accountId, String appId, String envId,
      String infraMappingId, TaskGroup taskGroup, List<ExecutionCapability> executionCapabilities,
      Map<String, String> taskSetupAbstractions);

  boolean isWhitelisted(DelegateTask task, String delegateId);

  boolean shouldValidate(DelegateTask task, String delegateId);
  List<String> connectedWhitelistedDelegates(DelegateTask task);

  List<String> extractSelectors(DelegateTask task);

  void refreshWhitelist(DelegateTask task, String delegateId);

  void saveConnectionResults(List<DelegateConnectionResult> results);

  String getActiveDelegateAssignmentErrorMessage(TaskFailureReason reason, DelegateTask delegateTask);

  List<Delegate> getAccountDelegates(String accountId);

  List<String> retrieveActiveDelegates(String accountId, BatchDelegateSelectionLog batch);

  boolean noInstalledDelegates(String accountId);

  List<String> getEligibleDelegatesToExecuteTask(DelegateTask task, BatchDelegateSelectionLog batch);

  List<String> getConnectedDelegateList(List<String> delegates, String accountId, BatchDelegateSelectionLog batch);

  boolean canAssignTask(BatchDelegateSelectionLog batch, String delegateId, DelegateTask task,
      Map<String, List<String>> nonAssignableDelegates);

  List<Delegate> fetchActiveDelegates(String accountId);
}
