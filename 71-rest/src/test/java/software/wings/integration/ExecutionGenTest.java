package software.wings.integration;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANUBHAW;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.security.SecureRandom;

/**
 * Created by rishi on 3/2/17.
 * This is meant to generate workflowExecutions for last 30 days
 */
@Integration
@Slf4j
public class ExecutionGenTest extends WingsBaseTest {
  private static final SecureRandom random = new SecureRandom();

  @Inject private WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void generateData() {
    PageRequest<WorkflowExecution> pageRequest = aPageRequest().withLimit("500").build();

    PageResponse<WorkflowExecution> response = wingsPersistence.query(WorkflowExecution.class, pageRequest);

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

      wingsPersistence.save(workflowExecution);
    }
    logger.info("response size: {}", response.size());
  }
}
