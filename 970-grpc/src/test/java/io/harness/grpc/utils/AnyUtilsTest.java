/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.utils;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.payloads.AggregatedUsage;
import io.harness.event.payloads.Lifecycle;
import io.harness.exception.DataFormatException;
import io.harness.rule.Owner;

import com.google.protobuf.Any;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AnyUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testToFqcnGivesCorrectClassName() throws Exception {
    Any any = Any.pack(Lifecycle.newBuilder().build());
    assertThat(AnyUtils.toFqcn(any)).isEqualTo(Lifecycle.class.getName());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testToClassGivesCorrectClass() throws Exception {
    Any any = Any.pack(Lifecycle.newBuilder().build());
    assertThat(AnyUtils.toClass(any)).isEqualTo(Lifecycle.class);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldThrowDataFormatExceptionIfUnpackingInvalidProto() throws Exception {
    assertThatExceptionOfType(DataFormatException.class)
        .isThrownBy(() -> AnyUtils.unpack(Any.pack(Lifecycle.newBuilder().build()), AggregatedUsage.class));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldUnpackValidProto() throws Exception {
    assertThatCode(() -> AnyUtils.unpack(Any.pack(Lifecycle.newBuilder().build()), Lifecycle.class))
        .doesNotThrowAnyException();
  }
}
