package io.harness.state.inspection;

import static java.util.Arrays.asList;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import io.harness.reflection.ReflectionUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StateInspectionServiceImpl implements StateInspectionService {
  private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

  @Inject HPersistence persistence;

  @Override
  public StateInspection get(String stateExecutionInstanceId) {
    return persistence.createQuery(StateInspection.class)
        .filter(StateInspection.STATE_EXECUTION_INSTANCE_ID_KEY, stateExecutionInstanceId)
        .get();
  }

  @Override
  public void append(String stateExecutionInstanceId, StateInspectionData data) {
    append(stateExecutionInstanceId, asList(data));
  }

  @Override
  public void append(String stateExecutionInstanceId, List<StateInspectionData> data) {
    // persistence.save(StateInspection.builder().stateExecutionInstanceId(stateExecutionInstanceId).build());

    final Query<StateInspection> query =
        persistence.createQuery(StateInspection.class)
            .filter(StateInspection.STATE_EXECUTION_INSTANCE_ID_KEY, stateExecutionInstanceId);

    final UpdateOperations<StateInspection> updateOperations =
        persistence.createUpdateOperations(StateInspection.class);

    updateOperations.setOnInsert(StateInspection.STATE_EXECUTION_INSTANCE_ID_KEY, stateExecutionInstanceId);

    for (StateInspectionData item : data) {
      updateOperations.set(StateInspection.DATA_KEY + "." + item.key(), item);
    }

    persistence.upsert(query, updateOperations);
  }
}
