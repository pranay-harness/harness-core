package io.harness.engine.interrupts;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.inject.Inject;

import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.execution.status.Status;
import org.awaitility.Awaitility;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.concurrent.TimeUnit;

public class InterruptTestHelper {
  @Inject private MongoTemplate mongoTemplate;

  public void waitForPlanStatus(String uuid, Status status) {
    Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = fetchPlanExecutionStatus(uuid);
      return planExecution != null && status == planExecution.getStatus();
    });
  }

  public void waitForPlanCompletion(String uuid) {
    final String finalStatusEnding = "ED";
    Awaitility.await().atMost(10, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = fetchPlanExecutionStatus(uuid);
      return planExecution != null && planExecution.getStatus().name().endsWith(finalStatusEnding);
    });
  }

  public PlanExecution fetchPlanExecutionStatus(String uuid) {
    Query query = query(where(PlanExecutionKeys.uuid).is(uuid));
    query.fields().include(PlanExecutionKeys.status);
    return mongoTemplate.findOne(query, PlanExecution.class);
  }
}
