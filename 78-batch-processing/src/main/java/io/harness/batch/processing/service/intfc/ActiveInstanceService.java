package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.entities.ActiveInstance;

import java.util.List;

public interface ActiveInstanceService {
  boolean create(ActiveInstance activeInstance);

  boolean delete(ActiveInstance activeInstance);

  List<ActiveInstance> fetchActiveInstances(String accountId, List<String> instanceIds);

  List<ActiveInstance> fetchActiveInstance(String accountId);

  ActiveInstance fetchActiveInstance(String accountId, String instanceId);
}
