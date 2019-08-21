package io.harness.event.handler.impl.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtils;
import software.wings.scheduler.events.segment.SegmentGroupEventJobContext;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collections;

public class SegmentGroupEventJobServiceTest extends BaseIntegrationTest {
  @Inject private WingsPersistence persistence;
  @Inject private SegmentGroupEventJobServiceImpl segmentGroupEventJobService;

  private boolean indexesEnsured;

  @Before
  public void ensureIndices() throws URISyntaxException {
    if (!indexesEnsured && !IntegrationTestUtils.isManagerRunning(client)) {
      persistence.getDatastore(SegmentGroupEventJobContext.class).ensureIndexes(SegmentGroupEventJobContext.class);
      indexesEnsured = true;
    }

    // delete existing entries in SegmentGroupEventJobContext's corresponding collection
    persistence.delete(persistence.createQuery(SegmentGroupEventJobContext.class));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSchedule() {
    String accountId = "some-account-" + SegmentGroupEventJobServiceTest.class.getSimpleName();
    long next = Instant.now().toEpochMilli();
    SegmentGroupEventJobContext ctx = new SegmentGroupEventJobContext(next, Collections.singletonList(accountId));
    persistence.save(ctx);

    SegmentGroupEventJobContext ctxFromDb = segmentGroupEventJobService.get(ctx.getUuid());

    assertThat(ctxFromDb).isNotNull();
    assertEquals(1, ctxFromDb.getAccountIds().size());

    accountId = "some-other-account-" + SegmentGroupEventJobServiceTest.class.getSimpleName();
    segmentGroupEventJobService.schedule(accountId, 10);

    int count = persistence.createQuery(SegmentGroupEventJobContext.class).asList().size();
    assertEquals(1, count);
    ctxFromDb = segmentGroupEventJobService.get(ctx.getUuid());
    assertThat(ctxFromDb).isNotNull();
    assertEquals(2, ctxFromDb.getAccountIds().size());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testScheduleNewDocumentIsCreated() {
    String accountId = "some-account-" + SegmentGroupEventJobServiceTest.class.getSimpleName();
    long next = Instant.now().toEpochMilli();
    SegmentGroupEventJobContext ctx = new SegmentGroupEventJobContext(next, Collections.singletonList(accountId));
    persistence.save(ctx);

    SegmentGroupEventJobContext ctxFromDb = segmentGroupEventJobService.get(ctx.getUuid());

    assertThat(ctxFromDb).isNotNull();
    assertEquals(1, ctxFromDb.getAccountIds().size());

    int count = persistence.createQuery(SegmentGroupEventJobContext.class).asList().size();
    assertEquals(1, count);

    accountId = "some-other-account-" + SegmentGroupEventJobServiceTest.class.getSimpleName();
    segmentGroupEventJobService.schedule(accountId, 1);

    count = persistence.createQuery(SegmentGroupEventJobContext.class).asList().size();
    assertEquals(2, count);
  }
}