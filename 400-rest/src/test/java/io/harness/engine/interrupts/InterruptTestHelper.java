package io.harness.engine.interrupts;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.pms.contracts.execution.Status;

import com.google.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class InterruptTestHelper {
  @Inject private MongoTemplate mongoTemplate;

  public void waitForPlanStatus(String uuid, Status status) {
    Awaitility.await().atMost(1, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = fetchPlanExecutionStatus(uuid);
      return planExecution != null && status == planExecution.getStatus();
    });
  }

  public void waitForNodeStatusInPlan(String planUuid, Status status) {
    Awaitility.await().atMost(1, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final List<NodeExecution> nodeExecutions = fetchNodeExecutionsByPlan(planUuid);
      return isNotEmpty(nodeExecutions) && nodeExecutions.stream().anyMatch(n -> n.getStatus() == status);
    });
  }

  public void waitForPlanCompletion(String uuid) {
    final String finalStatusEnding = "ED";
    Awaitility.await().atMost(3, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = fetchPlanExecutionStatus(uuid);
      return planExecution != null && planExecution.getStatus().name().endsWith(finalStatusEnding);
    });
  }

  public PlanExecution fetchPlanExecutionStatus(String uuid) {
    Query query = query(where(PlanExecutionKeys.uuid).is(uuid));
    query.fields().include(PlanExecutionKeys.status);
    return mongoTemplate.findOne(query, PlanExecution.class);
  }

  public List<NodeExecution> fetchNodeExecutionsByPlan(String planUuid) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planUuid));
    return mongoTemplate.find(query, NodeExecution.class);
  }
}
