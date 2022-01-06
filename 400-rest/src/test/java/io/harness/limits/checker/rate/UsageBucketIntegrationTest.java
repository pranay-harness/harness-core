/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.limits.checker.rate;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.limits.checker.rate.UsageBucket.UsageBucketKeys;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.integration.IntegrationTestBase;
import software.wings.integration.IntegrationTestUtils;

import com.google.inject.Inject;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class UsageBucketIntegrationTest extends IntegrationTestBase {
  @Inject private HPersistence persistence;

  private boolean indexesEnsured;

  private static final String KEY = "some-id-" + UsageBucketIntegrationTest.class.getSimpleName();

  @Before
  public void ensureIndices() throws Exception {
    if (!indexesEnsured && !IntegrationTestUtils.isManagerRunning(client)) {
      persistence.getDatastore(UsageBucket.class).ensureIndexes(UsageBucket.class);
      indexesEnsured = true;
    }
  }

  @After
  public void cleanUp() throws Exception {
    persistence.getDatastore(UsageBucket.class)
        .delete(persistence.createQuery(UsageBucket.class).filter(UsageBucketKeys.key, KEY));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testSerialization() {
    UsageBucket bucket = new UsageBucket(KEY, Arrays.asList(10L, 11L, 101L, 102L));

    String id = persistence.save(bucket);
    assertThat(id).isNotNull();

    UsageBucket fetchedBucket = persistence.get(UsageBucket.class, id);
    assertThat(fetchedBucket).isEqualTo(bucket);
  }
}
