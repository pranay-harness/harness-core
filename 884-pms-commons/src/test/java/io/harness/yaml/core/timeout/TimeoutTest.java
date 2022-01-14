package io.harness.yaml.core.timeout;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.SAMARTH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import java.util.Objects;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class TimeoutTest extends CategoryTest {
  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void fromStringTest() {
    // Valid cases
    assertThat(Objects.requireNonNull(Timeout.fromString("1m")).getTimeoutInMillis()).isEqualTo(60000);
    assertThat(Objects.requireNonNull(Timeout.fromString("1m 20s")).getTimeoutInMillis()).isEqualTo(80000);
    assertThat(Objects.requireNonNull(Timeout.fromString("1m20s")).getTimeoutInMillis()).isEqualTo(80000);

    // Invalid cases
    assertThatThrownBy(() -> Timeout.fromString("1m  20s")).isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> Timeout.fromString("1m 8")).isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> Timeout.fromString("18")).isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> Timeout.fromString("m")).isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> Timeout.fromString("20mm")).isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> Timeout.fromString("m20m")).isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> Timeout.fromString(" 1m")).isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> Timeout.fromString("1m ")).isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> Timeout.fromString("1 m")).isInstanceOf(InvalidArgumentsException.class);
    assertThatThrownBy(() -> Timeout.fromString("1a")).isInstanceOf(InvalidArgumentsException.class);
  }
}