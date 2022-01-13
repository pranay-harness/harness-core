/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.persistance;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.persistence.PersistentEntity;
import io.harness.rule.Owner;

import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.annotation.Id;

@OwnedBy(HarnessTeam.DX)
public class MongoEntityHelperTest extends CategoryTest {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testIsNewEntity() {
    final TestClass abc = TestClass.builder().id("abc").build();
    assertThat(MongoEntityUtils.isNewEntity(abc)).isFalse();

    final TestClass xyz = TestClass.builder().build();
    assertThat(MongoEntityUtils.isNewEntity(xyz)).isTrue();
  }

  @Builder
  public static class TestClass implements PersistentEntity {
    @Id String id;
  }
}
