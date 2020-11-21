package io.harness.service.impl;

import static java.lang.System.currentTimeMillis;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTaskService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateTaskServiceImpl implements DelegateTaskService {
  @Inject private HPersistence persistence;

  @Override
  public void touchExecutingTasks(String accountId, String delegateId, List<String> delegateTaskIds) {
    // Touch currently executing tasks.
    if (EmptyPredicate.isEmpty(delegateTaskIds)) {
      return;
    }

    log.info("Updating tasks");

    Query<DelegateTask> delegateTaskQuery = persistence.createQuery(DelegateTask.class)
                                                .filter(DelegateTaskKeys.accountId, accountId)
                                                .field(DelegateTaskKeys.uuid)
                                                .in(delegateTaskIds)
                                                .filter(DelegateTaskKeys.delegateId, delegateId)
                                                .filter(DelegateTaskKeys.status, DelegateTask.Status.STARTED)
                                                .project(DelegateTaskKeys.uuid, true)
                                                .project(DelegateTaskKeys.data_timeout, true);

    // TODO: it seems like mongo 4.2 supports update based on another field. Change this when we fully migrate to it.
    long now = currentTimeMillis();
    try (HIterator<DelegateTask> iterator = new HIterator<>(delegateTaskQuery.fetch())) {
      for (DelegateTask delegateTask : iterator) {
        persistence.update(delegateTask,
            persistence.createUpdateOperations(DelegateTask.class)
                .set(DelegateTaskKeys.expiry, now + delegateTask.getData().getTimeout()));
      }
    }
  }
}
