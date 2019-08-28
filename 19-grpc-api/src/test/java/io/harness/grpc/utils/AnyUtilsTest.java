package io.harness.grpc.utils;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;

import io.harness.category.element.UnitTests;
import io.harness.event.payloads.Lifecycle;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AnyUtilsTest {
  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void testToFqcnGivesCorrectClassName() throws Exception {
    Any any = Any.pack(Lifecycle.newBuilder().build());
    assertThat(AnyUtils.toFqcn(any)).isEqualTo(Lifecycle.class.getName());
  }

  @Test
  @Owner(emails = AVMOHAN)
  @Category(UnitTests.class)
  public void testToClassGivesCorrectClass() throws Exception {
    Any any = Any.pack(Lifecycle.newBuilder().build());
    assertThat(AnyUtils.toClass(any)).isEqualTo(Lifecycle.class);
  }
}