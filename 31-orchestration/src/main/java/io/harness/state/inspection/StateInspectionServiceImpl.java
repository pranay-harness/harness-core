package io.harness.state.inspection;

import static io.harness.persistence.HPersistence.UPSERT_RETURN_NEW_OPTIONS;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.observer.Subject;
import io.harness.persistence.HPersistence;
import io.harness.reflection.ReflectionUtils;
import lombok.Getter;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Singleton
public class StateInspectionServiceImpl implements StateInspectionService {
  private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

  @Inject HPersistence persistence;

  @Getter Subject<StateInspectionListener> subject = new Subject<>();

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
    final Query<StateInspection> query =
        persistence.createQuery(StateInspection.class)
            .filter(StateInspection.STATE_EXECUTION_INSTANCE_ID_KEY, stateExecutionInstanceId)
            .project(StateInspection.STATE_EXECUTION_INSTANCE_ID_KEY, true);

    final UpdateOperations<StateInspection> updateOperations =
        persistence.createUpdateOperations(StateInspection.class);

    updateOperations.setOnInsert(StateInspection.STATE_EXECUTION_INSTANCE_ID_KEY, stateExecutionInstanceId);

    for (StateInspectionData item : data) {
      updateOperations.set(StateInspection.DATA_KEY + "." + item.key(), item);
    }

    // TODO: there is a bug in morphia for obtaining the old value with projection. Change this to send notification
    //       only for inserts when this is fixed.
    persistence.upsert(query, updateOperations, UPSERT_RETURN_NEW_OPTIONS);

    subject.fireInform(StateInspectionListener::appendedDataFor, stateExecutionInstanceId);
  }
}