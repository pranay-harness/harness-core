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
            .project(StateExecutionInstance.CONTEXT_ELEMENT_KEY, true)
            .project(StateExecutionInstance.CONTEXT_TRANSITION_KEY, true)
            .project(StateExecutionInstance.DISPLAY_NAME_KEY, true)
            .project(StateExecutionInstance.EXECUTION_TYPE_KEY, true)
            .project(StateExecutionInstance.ID_KEY, true)
            .project(StateExecutionInstance.INTERRUPT_HISTORY_KEY, true)
            .project(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, true)
            .project(StateExecutionInstance.PREV_INSTANCE_ID_KEY, true)
            .project(StateExecutionInstance.STATE_EXECUTION_DATA_HISTORY_KEY, true)
            .project(StateExecutionInstance.STATE_EXECUTION_MAP_KEY, true)
            .project(StateExecutionInstance.STATE_NAME_KEY, true)
            .project(StateExecutionInstance.STATE_TYPE_KEY, true)
            .project(StateExecutionInstance.STATUS_KEY, true)
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
