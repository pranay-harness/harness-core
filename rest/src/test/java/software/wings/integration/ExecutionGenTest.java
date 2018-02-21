package software.wings.integration;

import static software.wings.common.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.KryoUtils;

import java.util.Random;

/**
 * Created by rishi on 3/2/17.
 * This is meant to generate workflowExecutions for last 30 days
 */
@Integration
@Ignore
public class ExecutionGenTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  private static final Logger logger = LoggerFactory.getLogger(ExecutionGenTest.class);

  @Test
  @Ignore
  public void generateData() {
    PageRequest<WorkflowExecution> pageRequest = PageRequest.Builder.aPageRequest().withLimit("500").build();

    PageResponse<WorkflowExecution> response = wingsPersistence.query(WorkflowExecution.class, pageRequest);

    Random random = new Random();
    for (int i = 0; i < 50; i++) {
      WorkflowExecution workflowExecution = KryoUtils.clone(response.get(random.nextInt(response.size())));
      if (workflowExecution.getStatus() != ExecutionStatus.SUCCESS
          && workflowExecution.getStatus() != ExecutionStatus.FAILED) {
        continue;
      }
      workflowExecution.setUuid(generateUuid());
      int day = random.nextInt(30);
      long interval = day * 24 * 3600 * 1000;
      workflowExecution.setCreatedAt(workflowExecution.getCreatedAt() - interval);
      workflowExecution.setLastUpdatedAt(workflowExecution.getLastUpdatedAt() - interval);

      wingsPersistence.save(workflowExecution);
    }
    logger.info("response size: {}", response.size());
  }
}
