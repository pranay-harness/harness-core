package software.wings.sm;

import io.harness.beans.ExecutionStatus;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;

public class StateExecutionInstanceHelper {
  @Inject KryoSerializer kryoSerializer;

  public StateExecutionInstance clone(StateExecutionInstance instance) {
    StateExecutionInstance clone = kryoSerializer.clone(instance);
    clone.setPrevInstanceId(null);
    clone.setDelegateTaskId(null);
    clone.setContextTransition(true);
    clone.setStatus(ExecutionStatus.NEW);
    clone.setStartTs(null);
    clone.setEndTs(null);
    clone.setCreatedAt(0);
    clone.setLastUpdatedAt(0);
    clone.setHasInspection(false);
    return clone;
  }
}
