/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.handler.impl.segment;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.integration.IntegrationTestBase;
import software.wings.integration.IntegrationTestUtils;
import software.wings.scheduler.events.segment.SegmentGroupEventJobContext;

import com.google.inject.Inject;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collections;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SegmentGroupEventJobServiceTest extends IntegrationTestBase {
  @Inject private HPersistence persistence;
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
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testSchedule() {
    String accountId = "some-account-" + SegmentGroupEventJobServiceTest.class.getSimpleName();
    long next = Instant.now().toEpochMilli();
    SegmentGroupEventJobContext ctx = new SegmentGroupEventJobContext(next, Collections.singletonList(accountId));
    persistence.save(ctx);

    SegmentGroupEventJobContext ctxFromDb = segmentGroupEventJobService.get(ctx.getUuid());

    assertThat(ctxFromDb).isNotNull();
    assertThat(ctxFromDb.getAccountIds()).hasSize(1);

    accountId = "some-other-account-" + SegmentGroupEventJobServiceTest.class.getSimpleName();
    segmentGroupEventJobService.schedule(accountId, 10);

    int count = persistence.createQuery(SegmentGroupEventJobContext.class).asList().size();
    assertThat(count).isEqualTo(1);
    ctxFromDb = segmentGroupEventJobService.get(ctx.getUuid());
    assertThat(ctxFromDb).isNotNull();
    assertThat(ctxFromDb.getAccountIds()).hasSize(2);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testScheduleNewDocumentIsCreated() {
    String accountId = "some-account-" + SegmentGroupEventJobServiceTest.class.getSimpleName();
    long next = Instant.now().toEpochMilli();
    SegmentGroupEventJobContext ctx = new SegmentGroupEventJobContext(next, Collections.singletonList(accountId));
    persistence.save(ctx);

    SegmentGroupEventJobContext ctxFromDb = segmentGroupEventJobService.get(ctx.getUuid());

    assertThat(ctxFromDb).isNotNull();
    assertThat(ctxFromDb.getAccountIds()).hasSize(1);

    int count = persistence.createQuery(SegmentGroupEventJobContext.class).asList().size();
    assertThat(count).isEqualTo(1);

    accountId = "some-other-account-" + SegmentGroupEventJobServiceTest.class.getSimpleName();
    segmentGroupEventJobService.schedule(accountId, 1);

    count = persistence.createQuery(SegmentGroupEventJobContext.class).asList().size();
    assertThat(count).isEqualTo(2);
  }
}
