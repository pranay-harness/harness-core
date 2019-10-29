package io.harness.grpc.utils;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.protobuf.Any;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.payloads.Lifecycle;
import io.harness.exception.DataFormatException;
import io.harness.perpetualtask.example.SamplePerpetualTaskParams;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AnyUtilsTest extends CategoryTest {
  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void testToFqcnGivesCorrectClassName() throws Exception {
    Any any = Any.pack(Lifecycle.newBuilder().build());
    assertThat(AnyUtils.toFqcn(any)).isEqualTo(Lifecycle.class.getName());
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void testToClassGivesCorrectClass() throws Exception {
    Any any = Any.pack(Lifecycle.newBuilder().build());
    assertThat(AnyUtils.toClass(any)).isEqualTo(Lifecycle.class);
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void shouldThrowDataFormatExceptionIfUnpackingInvalidProto() throws Exception {
    assertThatExceptionOfType(DataFormatException.class)
        .isThrownBy(() -> AnyUtils.unpack(Any.pack(Lifecycle.newBuilder().build()), SamplePerpetualTaskParams.class));
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void shouldUnpackValidProto() throws Exception {
    assertThatCode(() -> AnyUtils.unpack(Any.pack(Lifecycle.newBuilder().build()), Lifecycle.class))
        .doesNotThrowAnyException();
  }
}