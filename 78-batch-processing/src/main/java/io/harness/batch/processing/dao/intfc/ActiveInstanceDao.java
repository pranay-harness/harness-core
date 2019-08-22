package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.entities.ActiveInstance;

import java.util.List;

public interface ActiveInstanceDao {
  boolean create(ActiveInstance activeInstance);

  boolean delete(ActiveInstance activeInstance);

  List<ActiveInstance> fetchActiveInstances(String accountId, List<String> instanceIds);

  List<ActiveInstance> fetchActiveInstance(String accountId);

  ActiveInstance fetchActiveInstance(String accountId, String instanceId);
}
