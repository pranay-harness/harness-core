/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.core;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.RecasterTestBase;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exceptions.DuplicateAliasException;
import io.harness.rule.Owner;
import io.harness.utils.RecastReflectionUtils;

import java.util.LinkedHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class AliasRegistryTest extends RecasterTestBase {
  private static final String DUMMY_WITH_ALIAS = "dummyWithAlias";

  private final AliasRegistry aliasRegistry = AliasRegistry.getInstance();

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    aliasRegistry.register(DummyWithoutAlias.class);
    assertThat(aliasRegistry.obtain(DummyWithoutAlias.class.getName())).isNull();

    aliasRegistry.register(DummyWithAlias.class);
    assertThat(aliasRegistry.obtain(RecastReflectionUtils.obtainRecasterAliasValueOrNull(DummyWithAlias.class)))
        .isEqualTo(DummyWithAlias.class);

    assertThatThrownBy(() -> aliasRegistry.register(DummyWithAliasDuplicated.class))
        .isInstanceOf(DuplicateAliasException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestShouldContainAlias() {
    aliasRegistry.addPackages("io.harness");

    assertThat(aliasRegistry.shouldContainAlias(DummyWithoutAlias.class)).isTrue();
    assertThat(aliasRegistry.shouldContainAlias(LinkedHashMap.class)).isFalse();
  }

  @Builder
  @AllArgsConstructor
  private static class DummyWithoutAlias {
    private final String s;
  }

  @Builder
  @AllArgsConstructor
  @RecasterAlias(DUMMY_WITH_ALIAS)
  private static class DummyWithAlias {
    private final String s;
  }

  @Builder
  @AllArgsConstructor
  @RecasterAlias(DUMMY_WITH_ALIAS)
  private static class DummyWithAliasDuplicated {
    private final String s;
  }
}
