package io.harness.engine.interrupts;

import com.google.inject.Inject;

import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.persistence.HPersistence;
import org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

public class InterruptTestHelper {
  @Inject HPersistence hPersistence;

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
    return hPersistence.createQuery(PlanExecution.class)
        .filter(PlanExecutionKeys.uuid, uuid)
        .project(PlanExecutionKeys.status, true)
        .get();
  }
}
