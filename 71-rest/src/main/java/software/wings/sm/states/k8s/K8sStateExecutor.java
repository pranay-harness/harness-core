package software.wings.sm.states.k8s;

import static java.time.Duration.ofMinutes;

import com.google.common.primitives.Ints;

import io.harness.delegate.beans.ResponseData;
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

  default Integer getTimeoutMillisFromMinutes(Integer timeoutMinutes) {
    if (timeoutMinutes == null) {
      return null;
    }
    return Ints.checkedCast(ofMinutes(timeoutMinutes).toMillis());
  }
}
