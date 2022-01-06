/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.mongo;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PersistenceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.persistence.LogKeyUtils;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LogKeyUtilsTest extends PersistenceTestBase {
  static class DummySampleEntity extends SampleEntity {}

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCalculateEntityIdName() {
    assertThat(LogKeyUtils.calculateLogKeyForId(SampleEntity.class)).isEqualTo("sampleEntityId");
    assertThat(LogKeyUtils.calculateLogKeyForId(DummySampleEntity.class)).isEqualTo("sampleEntityId");

    assertThat(LogKeyUtils.calculateLogKeyForId(Object.class)).isEqualTo("");
  }
}
