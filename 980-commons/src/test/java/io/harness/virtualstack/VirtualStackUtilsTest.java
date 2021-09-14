/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.virtualstack;

import static io.harness.manage.GlobalContextManager.initGlobalContextGuard;
import static io.harness.manage.GlobalContextManager.upsertGlobalContextRecord;
import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CommonsTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.context.MdcGlobalContextData;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
public class VirtualStackUtilsTest extends CommonsTestBase {
  @Inject KryoSerializer kryoSerializer;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testPopulateRequest() {
    VirtualStackRequest virtualStackRequest = VirtualStackUtils.populateRequest(kryoSerializer);
    // expect empty map
    assertThat(virtualStackRequest.getGlobalContext().size()).isEqualTo(9);

    initGlobalContextGuard(null);
    upsertGlobalContextRecord(MdcGlobalContextData.builder().map(ImmutableMap.of("foo", "bar")).build());

    virtualStackRequest = VirtualStackUtils.populateRequest(kryoSerializer);
    // expect map with some data
    assertThat(virtualStackRequest.getGlobalContext().size()).isGreaterThan(9);

    upsertGlobalContextRecord(() -> "foo");

    virtualStackRequest = VirtualStackUtils.populateRequest(kryoSerializer);
    // expect no object, because kryo throws exception
    assertThat(virtualStackRequest.getGlobalContext().size()).isEqualTo(0);
  }
}
