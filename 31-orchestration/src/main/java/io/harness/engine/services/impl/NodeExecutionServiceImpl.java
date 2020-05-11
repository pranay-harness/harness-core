package io.harness.engine.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
public class NodeExecutionServiceImpl implements NodeExecutionService {
  @Inject @Named("enginePersistence") HPersistence hPersistence;

  @Override
  public List<NodeExecution> fetchNodeExecutions(String planExecutionId) {
    List<NodeExecution> nodeExecutions = new ArrayList<>();
    Query<NodeExecution> nodeExecutionQuery = hPersistence.createQuery(NodeExecution.class, excludeAuthority)
                                                  .filter(NodeExecutionKeys.planExecutionId, planExecutionId);
    try (HIterator<NodeExecution> nodeExecutionIterator = new HIterator<>(nodeExecutionQuery.fetch())) {
      while (nodeExecutionIterator.hasNext()) {
        nodeExecutions.add(nodeExecutionIterator.next());
      }
    }
    return nodeExecutions;
  }
}
