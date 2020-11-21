package io.harness.engine;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class EngineTestHelper {
  @Inject @Named("orchestrationMongoTemplate") MongoTemplate mongoTemplate;

  public void waitForPlanCompletion(String uuid) {
    final String finalStatusEnding = "ED";
    Awaitility.await().atMost(15, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = getPlanExecutionStatus(uuid);
      return planExecution != null && planExecution.getStatus().name().endsWith(finalStatusEnding);
    });
  }

  public PlanExecution getPlanExecutionStatus(String uuid) {
    Query query = query(where(PlanExecutionKeys.uuid).is(uuid));
    query.fields().include(PlanExecutionKeys.status);
    return mongoTemplate.findOne(query, PlanExecution.class);
  }
}
