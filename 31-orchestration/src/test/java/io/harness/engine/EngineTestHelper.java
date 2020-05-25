package io.harness.engine;

import com.google.inject.Inject;

import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.persistence.HPersistence;
import org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

public class EngineTestHelper {
  @Inject HPersistence hPersistence;

  public void waitForPlanCompletion(String uuid) {
    final String finalStatusEnding = "ED";
    Awaitility.await().atMost(15, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = getPlanExecutionStatus(uuid);
      return planExecution != null && planExecution.getStatus().name().endsWith(finalStatusEnding);
    });
  }

  public PlanExecution getPlanExecutionStatus(String uuid) {
    return hPersistence.createQuery(PlanExecution.class)
        .filter(PlanExecutionKeys.uuid, uuid)
        .project(PlanExecutionKeys.status, true)
        .get();
  }
}
