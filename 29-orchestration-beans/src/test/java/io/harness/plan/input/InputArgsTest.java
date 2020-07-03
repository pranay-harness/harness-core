package io.harness.plan.input;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InputArgsTest extends OrchestrationBeansTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testContainKey() {
    InputArgs inputArgs = prepareInputArgs();
    assertThat(inputArgs.containsKey("random")).isFalse();
    assertThat(inputArgs.containsKey("a")).isTrue();
    assertThat(inputArgs.containsKey("b")).isTrue();
    assertThat(inputArgs.containsKey("c")).isTrue();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGet() {
    InputArgs inputArgs = prepareInputArgs();
    assertThat(inputArgs.get("a")).isInstanceOf(DummyOutput.class);
    assertThat(((DummyOutput) inputArgs.get("a")).getTest()).isEqualTo("t1");
    assertThat(inputArgs.get("b")).isInstanceOf(String.class);
    assertThat(inputArgs.get("b")).isEqualTo("t2");
    assertThat(inputArgs.get("c")).isInstanceOf(String.class);
    assertThat(inputArgs.get("c")).isEqualTo("t3");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testKeySet() {
    InputArgs inputArgs = prepareInputArgs();
    assertThat(inputArgs.keySet()).containsExactlyInAnyOrder("a", "b", "c");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testStrMap() {
    InputArgs inputArgs = prepareInputArgs();
    assertThat(inputArgs.strMap().keySet()).containsExactlyInAnyOrder("b", "c");
  }

  private InputArgs prepareInputArgs() {
    return InputArgs.builder()
        .put("a", DummyOutput.builder().test("t1").build())
        .putAll(ImmutableMap.of("b", "t2", "c", DummyOutput.builder().test("t3").build()))
        .put("c", "t3")
        .build();
  }
}
