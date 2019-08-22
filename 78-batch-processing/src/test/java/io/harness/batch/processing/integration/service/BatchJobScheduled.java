package io.harness.batch.processing.integration.service;

import static io.harness.rule.OwnerRule.HITESH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.batch.processing.entities.BatchJobScheduledData.BatchJobScheduledDataKeys;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import io.harness.category.element.IntegrationTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.OwnerRule.Owner;
import lombok.val;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class BatchJobScheduled {
  @Autowired private BatchJobScheduledDataService batchJobScheduledDataService;

  @Autowired private HPersistence hPersistence;

  @Test
  @Owner(emails = HITESH)
  @Category(IntegrationTests.class)
  @Ignore("TODO: Not running it now, will run it in next iteration.")
  public void shouldCreateBatchJobScheduledData() {
    Instant firstStartAt = Instant.now().minus(2, ChronoUnit.DAYS);
    Instant firstEndAt = Instant.now().minus(1, ChronoUnit.DAYS);
    BatchJobScheduledData batchJobScheduledData =
        new BatchJobScheduledData(BatchJobType.ECS_EVENT, firstStartAt, firstEndAt);
    boolean save = batchJobScheduledDataService.create(batchJobScheduledData);
    assertTrue(save);

    Instant instant = batchJobScheduledDataService.fetchLastBatchJobScheduledTime(BatchJobType.ECS_EVENT);
    assertEquals(firstEndAt, instant);

    Instant secondStartAt = firstEndAt;
    Instant secondEndAt = Instant.now();
    batchJobScheduledData = new BatchJobScheduledData(BatchJobType.ECS_EVENT, secondStartAt, secondEndAt);
    save = batchJobScheduledDataService.create(batchJobScheduledData);
    assertTrue(save);

    instant = batchJobScheduledDataService.fetchLastBatchJobScheduledTime(BatchJobType.ECS_EVENT);
    assertEquals(secondEndAt, instant);
  }

  @After
  public void clearCollection() {
    val ds = hPersistence.getDatastore(BatchJobScheduledData.class);
    ds.delete(ds.createQuery(BatchJobScheduledData.class)
                  .filter(BatchJobScheduledDataKeys.batchJobType, BatchJobType.ECS_EVENT));
  }
}
