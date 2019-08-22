package io.harness.batch.processing.dao.impl;

import io.harness.batch.processing.dao.intfc.ActiveInstanceDao;
import io.harness.batch.processing.entities.ActiveInstance;
import io.harness.batch.processing.entities.ActiveInstance.ActiveInstanceKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class ActiveInstanceDaoImpl implements ActiveInstanceDao {
  @Autowired private HPersistence hPersistence;

  @Override
  public boolean create(ActiveInstance activeInstance) {
    return hPersistence.save(activeInstance) != null;
  }

  @Override
  public boolean delete(ActiveInstance activeInstance) {
    return hPersistence.delete(ActiveInstance.class, activeInstance.getUuid());
  }

  @Override
  public List<ActiveInstance> fetchActiveInstances(String accountId, List<String> instanceIds) {
    return hPersistence.createQuery(ActiveInstance.class)
        .filter(ActiveInstanceKeys.accountId, accountId)
        .field(ActiveInstanceKeys.instanceId)
        .in(instanceIds)
        .order(Sort.ascending(ActiveInstanceKeys.createdAt))
        .asList();
  }

  @Override
  public List<ActiveInstance> fetchActiveInstance(String accountId) {
    return hPersistence.createQuery(ActiveInstance.class)
        .filter(ActiveInstanceKeys.accountId, accountId)
        .order(Sort.ascending(ActiveInstanceKeys.createdAt))
        .asList();
  }

  @Override
  public ActiveInstance fetchActiveInstance(String accountId, String instanceId) {
    return hPersistence.createQuery(ActiveInstance.class)
        .filter(ActiveInstanceKeys.accountId, accountId)
        .filter(ActiveInstanceKeys.instanceId, instanceId)
        .get();
  }
}
