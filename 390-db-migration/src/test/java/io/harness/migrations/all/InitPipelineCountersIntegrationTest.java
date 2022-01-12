/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.rule.Owner;

import software.wings.beans.Pipeline;
import software.wings.integration.IntegrationTestBase;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InitPipelineCountersIntegrationTest extends IntegrationTestBase {
  @Inject private InitPipelineCounters initPipelineCounters;

  @Before
  public void init() {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_PIPELINE.toString()));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testMigrate() {
    long pipelineCount = wingsPersistence.createQuery(Pipeline.class).count();
    if (pipelineCount == 0) {
      return;
    }

    long initialCount = wingsPersistence.createQuery(Counter.class)
                            .field("key")
                            .endsWith(ActionType.CREATE_PIPELINE.toString())
                            .count();

    assertThat(initialCount).isEqualTo(0);
    initPipelineCounters.migrate();

    long finalCount = wingsPersistence.createQuery(Counter.class)
                          .field("key")
                          .endsWith(ActionType.CREATE_PIPELINE.toString())
                          .count();

    assertThat(0).isNotEqualTo(finalCount);
  }
}
