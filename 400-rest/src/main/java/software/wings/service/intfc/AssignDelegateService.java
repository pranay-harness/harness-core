/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.TaskFailureReason;
import io.harness.selection.log.BatchDelegateSelectionLog;

import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.List;
import java.util.Map;

@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
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

  String pickFirstAttemptDelegate(DelegateTask task);

  void refreshWhitelist(DelegateTask task, String delegateId);

  void saveConnectionResults(List<DelegateConnectionResult> results);

  void clearConnectionResults(String accountId, String delegateId);

  void clearConnectionResults(String accountId);

  String getActiveDelegateAssignmentErrorMessage(TaskFailureReason reason, DelegateTask delegateTask);

  List<Delegate> getAccountDelegates(String accountId);

  List<String> retrieveActiveDelegates(String accountId, BatchDelegateSelectionLog batch);

  boolean noInstalledDelegates(String accountId);
}
