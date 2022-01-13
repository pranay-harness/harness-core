/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
