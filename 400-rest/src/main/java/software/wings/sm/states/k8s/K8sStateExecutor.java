package software.wings.sm.states.k8s;

import io.harness.beans.DelegateTask;
import io.harness.tasks.ResponseData;

import software.wings.beans.command.CommandUnit;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;

import java.util.List;
import java.util.Map;

public interface K8sStateExecutor {
  void validateParameters(ExecutionContext context);

  String commandName();

  String stateType();

  List<CommandUnit> commandUnitList(boolean remoteStoreType);

  ExecutionResponse executeK8sTask(ExecutionContext context, String activityId);

  ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response);

  void handleDelegateTask(ExecutionContext context, DelegateTask delegateTask);
}
