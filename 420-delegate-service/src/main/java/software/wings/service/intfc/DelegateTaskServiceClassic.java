/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.delegate.beans.DelegateProgressData;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskAbortEvent;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.validation.Create;

import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

@OwnedBy(DEL)
@BreakDependencyOn("software.wings.service.intfc.ownership.OwnedByAccount")
public interface DelegateTaskServiceClassic extends OwnedByAccount {
  @ValidationGroups(Create.class) String queueTask(@Valid DelegateTask task);

  void scheduleSyncTask(DelegateTask task);

  <T extends DelegateResponseData> T executeTask(DelegateTask task) throws InterruptedException;

  void saveDelegateTask(DelegateTask task, DelegateTask.Status status);

  String queueParkedTask(String accountId, String taskId);

  byte[] getParkedTaskResults(String accountId, String taskId, String driverId);

  DelegateTaskPackage acquireDelegateTask(String accountId, String delegateId, String taskId);

  DelegateTaskPackage reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results);

  void failIfAllDelegatesFailed(String accountId, String delegateId, String taskId, boolean areClientToolsInstalled);

  void publishTaskProgressResponse(
      String accountId, String driverId, String delegateTaskId, DelegateProgressData responseData);

  boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent);

  DelegateTask abortTask(String accountId, String delegateTaskId);

  String expireTask(String accountId, String delegateTaskId);

  List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly);

  Optional<DelegateTask> fetchDelegateTask(String accountId, String taskId);

  void convertToExecutionCapability(DelegateTask task);

  void executeBatchCapabilityCheckTask(String accountId, String delegateId,
      List<CapabilitySubjectPermission> capabilitySubjectPermissions, String blockedTaskSelectionDetailsId);

  String obtainCapableDelegateId(DelegateTask task, Set<String> alreadyTriedDelegates);

  boolean checkDelegateConnected(String accountId, String delegateId);
}
