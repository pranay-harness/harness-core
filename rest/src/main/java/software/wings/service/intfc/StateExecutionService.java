package software.wings.service.intfc;

import software.wings.sm.StateExecutionInstance;

import java.util.Map;

public interface StateExecutionService {
  Map<String, StateExecutionInstance> executionStatesMap(String appId, String executionUuid);
}
