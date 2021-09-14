/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.core.registries;

import static io.harness.rule.OwnerRule.BRIJESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.execution.expression.SdkFunctor;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FunctorRegistryTest extends PmsSdkCoreTestBase {
  @Inject private FunctorRegistry functorRegistry;

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void shouldTestRegistry() {
    String functorKey = "dummy";
    functorRegistry.register(functorKey, new DummySdkFunctor());
    SdkFunctor functor = functorRegistry.obtain(functorKey);
    assertThat(functor).isNotNull();

    assertThatThrownBy(() -> functorRegistry.register(functorKey, new DummySdkFunctor()))
        .isInstanceOf(DuplicateRegistryException.class);

    assertThatThrownBy(() -> functorRegistry.obtain("RANDOM")).isInstanceOf(UnregisteredKeyAccessException.class);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    assertThat(functorRegistry.getType()).isEqualTo(RegistryType.SDK_FUNCTOR.name());
  }

  @Value
  @Builder
  private static class DummySdkFunctor implements SdkFunctor {}
}
