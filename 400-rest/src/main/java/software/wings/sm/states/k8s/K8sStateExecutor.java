/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.tasks.ResponseData;

import software.wings.beans.command.CommandUnit;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;

import java.util.List;
import java.util.Map;

@OwnedBy(CDP)
public interface K8sStateExecutor {
  void validateParameters(ExecutionContext context);

  String commandName();

  String stateType();

  List<CommandUnit> commandUnitList(boolean remoteStoreType, String accountId);

  ExecutionResponse executeK8sTask(ExecutionContext context, String activityId);

  ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response);

  void handleDelegateTask(ExecutionContext context, DelegateTask delegateTask);
}
