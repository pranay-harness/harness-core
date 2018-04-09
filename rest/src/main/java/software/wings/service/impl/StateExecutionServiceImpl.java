package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBCursor;
import org.mongodb.morphia.query.MorphiaIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.StateExecutionService;
import software.wings.sm.StateExecutionInstance;

import java.util.HashMap;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class StateExecutionServiceImpl implements StateExecutionService {
  private static final Logger logger = LoggerFactory.getLogger(StateExecutionServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  public Map<String, StateExecutionInstance> executionStatesMap(String appId, String executionUuid) {
    Map<String, StateExecutionInstance> allInstancesIdMap = new HashMap<>();

    final MorphiaIterator<StateExecutionInstance, StateExecutionInstance> stateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, appId)
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, executionUuid)
            .project(StateExecutionInstance.CONTEXT_ELEMENTS_KEY, false)
            .project(StateExecutionInstance.CALLBACK, false)
            .fetch();

    try (DBCursor cursor = stateExecutionInstances.getCursor()) {
      while (stateExecutionInstances.hasNext()) {
        StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
        allInstancesIdMap.put(stateExecutionInstance.getUuid(), stateExecutionInstance);
      }
    }
    return allInstancesIdMap;
  }
}
