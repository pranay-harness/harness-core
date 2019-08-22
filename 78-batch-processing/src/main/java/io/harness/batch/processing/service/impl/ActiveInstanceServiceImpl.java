package io.harness.batch.processing.service.impl;

import io.harness.batch.processing.dao.intfc.ActiveInstanceDao;
import io.harness.batch.processing.entities.ActiveInstance;
import io.harness.batch.processing.service.intfc.ActiveInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ActiveInstanceServiceImpl implements ActiveInstanceService {
  @Autowired private ActiveInstanceDao activeInstanceDao;

  @Override
  public boolean create(ActiveInstance activeInstance) {
    return activeInstanceDao.create(activeInstance);
  }

  @Override
  public boolean delete(ActiveInstance activeInstance) {
    return activeInstanceDao.delete(activeInstance);
  }

  @Override
  public List<ActiveInstance> fetchActiveInstances(String accountId, List<String> instanceIds) {
    return activeInstanceDao.fetchActiveInstances(accountId, instanceIds);
  }

  @Override
  public List<ActiveInstance> fetchActiveInstance(String accountId) {
    return activeInstanceDao.fetchActiveInstance(accountId);
  }

  @Override
  public ActiveInstance fetchActiveInstance(String accountId, String instanceId) {
    return activeInstanceDao.fetchActiveInstance(accountId, instanceId);
  }
}
