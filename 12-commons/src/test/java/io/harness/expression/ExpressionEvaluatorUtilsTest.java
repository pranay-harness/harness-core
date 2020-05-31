package io.harness.expression;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;
import java.util.Optional;

public class ExpressionEvaluatorUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchField() {
    Map<String, Object> map = ImmutableMap.of("a", "aVal", "b", DummyA.builder().strVal("bVal").build());
    Optional<Object> optional = ExpressionEvaluatorUtils.fetchField(map, "a");
    assertThat(optional).isPresent();
    assertThat(optional.get()).isEqualTo("aVal");

    optional = ExpressionEvaluatorUtils.fetchField(map, "b");
    assertThat(optional).isPresent();
    assertThat(optional.get()).isInstanceOf(DummyA.class);
    assertThat(((DummyA) optional.get()).getStrVal()).isEqualTo("bVal");

    DummyA dummyA = DummyA.builder().strVal("a").intVal(1).pairVal(Pair.of("b", "c")).build();
    optional = ExpressionEvaluatorUtils.fetchField(dummyA, "strVal");
    assertThat(optional).isPresent();
    assertThat(optional.get()).isEqualTo("a");

    optional = ExpressionEvaluatorUtils.fetchField(dummyA, "intVal");
    assertThat(optional).isPresent();
    assertThat(optional.get()).isEqualTo(1);

    optional = ExpressionEvaluatorUtils.fetchField(dummyA, "pairVal");
    assertThat(optional).isPresent();
    assertThat(optional.get()).isEqualTo(Pair.of("b", "c"));
  }

  @Value
  @Builder
  public static class DummyA {
    String strVal;
    int intVal;
    Pair<String, String> pairVal;
  }
}
