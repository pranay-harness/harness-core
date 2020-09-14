package io.harness.expression;

import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.ParameterField;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.function.Function;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testUpdateStringFieldValues() {
    Object obj = ExpressionEvaluatorUtils.updateExpressions(null, DummyFunctor.builder().build());
    assertThat(obj).isNull();

    String original = "original";
    String originalObject = "originalObject";
    String originalInt1 = "originalInt1";
    String originalInt2 = "originalInt2";
    String updated = "updated";
    List<Pair<String, String>> pairs =
        asList(ImmutablePair.of(original, original), ImmutablePair.of(original, original));
    Map<String, Object> map = ImmutableMap.of("a", original, "b", 2, "c", ImmutablePair.of(1, original));
    Set<String> set = ImmutableSet.of("a", original);
    DummyB dummyBInternal = DummyB.builder().strVal(original).strValIgnored(original).build();
    String[][] strArrArr = new String[][] {new String[] {"a", original, "b"}, new String[] {"c", original, original}};
    DummyB dummyB = DummyB.builder()
                        .pairs(pairs)
                        .map(map)
                        .set(set)
                        .intVal(5)
                        .strVal(original)
                        .strValIgnored(original)
                        .obj(dummyBInternal)
                        .strArrArr(strArrArr)
                        .build();
    dummyBInternal.setObj(dummyB);

    DummyC dummyC1 = DummyC.builder()
                         .dummyC(ParameterField.createExpressionField(true, "random", null))
                         .strVal1(ParameterField.createValueField(original))
                         .strVal2(original)
                         .intVal1(ParameterField.createExpressionField(true, originalInt1, null))
                         .intVal2(15)
                         .build();
    dummyB.setDummyC1(ParameterField.createExpressionField(true, originalObject, null));

    DummyC dummyC2 = DummyC.builder()
                         .strVal1(ParameterField.createValueField(original))
                         .strVal2(original)
                         .intVal1(ParameterField.createExpressionField(true, originalInt2, null))
                         .intVal2(20)
                         .build();
    dummyB.setDummyC2(dummyC2);

    Map<String, Object> context =
        ImmutableMap.of(original, updated, originalObject, dummyC1, originalInt1, 10, originalInt2, 15);
    ExpressionEvaluatorUtils.updateExpressions(dummyB, DummyFunctor.builder().context(context).build());
    assertThat(dummyB.getDummyC1().isExpression()).isFalse();
    assertThat(dummyB.getDummyC1().getValue()).isEqualTo(dummyC1);
    assertThat(dummyC1.dummyC.isExpression()).isTrue();
    assertThat(dummyC1.strVal1.isExpression()).isFalse();
    assertThat(dummyC1.strVal1.getValue()).isEqualTo(updated);
    assertThat(dummyC1.strVal2).isEqualTo(updated);
    assertThat(dummyC1.intVal1.isExpression()).isFalse();
    assertThat(dummyC1.intVal1.getValue()).isEqualTo(10);
    assertThat(dummyC1.intVal2).isEqualTo(15);
    assertThat(dummyB.getDummyC2()).isEqualTo(dummyC2);
    assertThat(dummyC2.strVal1.isExpression()).isFalse();
    assertThat(dummyC2.strVal1.getValue()).isEqualTo(updated);
    assertThat(dummyC2.strVal2).isEqualTo(updated);
    assertThat(dummyC2.intVal1.isExpression()).isFalse();
    assertThat(dummyC2.intVal1.getValue()).isEqualTo(15);
    assertThat(dummyC2.intVal2).isEqualTo(20);
    assertThat(pairs.get(0).getLeft()).isEqualTo(updated);
    assertThat(pairs.get(0).getRight()).isEqualTo(updated);
    assertThat(pairs.get(1).getLeft()).isEqualTo(updated);
    assertThat(pairs.get(1).getRight()).isEqualTo(updated);
    assertThat(map.get("a")).isEqualTo(updated);
    assertThat(map.get("b")).isEqualTo(2);
    assertThat(set).containsExactlyInAnyOrder("a", updated);
    assertThat(((Pair<Integer, String>) map.get("c")).getLeft()).isEqualTo(1);
    assertThat(((Pair<Integer, String>) map.get("c")).getRight()).isEqualTo(updated);
    assertThat(dummyB.getStrVal()).isEqualTo(updated);
    assertThat(dummyB.getStrValIgnored()).isEqualTo(original);
    assertThat(dummyB.getObj()).isNotNull();
    assertThat(dummyBInternal.getPairs()).isNull();
    assertThat(dummyBInternal.getMap()).isNull();
    assertThat(dummyBInternal.getIntVal()).isEqualTo(0);
    assertThat(dummyBInternal.getStrVal()).isEqualTo(updated);
    assertThat(dummyBInternal.getStrValIgnored()).isEqualTo(original);
    assertThat(dummyBInternal.getObj()).isEqualTo(dummyB);
    assertThat(dummyBInternal.getStrArrArr()).isNull();
    assertThat(strArrArr[0][0]).isEqualTo("a");
    assertThat(strArrArr[0][1]).isEqualTo(updated);
    assertThat(strArrArr[0][2]).isEqualTo("b");
    assertThat(strArrArr[1][0]).isEqualTo("c");
    assertThat(strArrArr[1][1]).isEqualTo(updated);
    assertThat(strArrArr[1][2]).isEqualTo(updated);
  }

  @Value
  @Builder
  private static class DummyFunctor implements ExpressionResolveFunctor {
    Map<String, Object> context;

    @Override
    public String renderExpression(String str) {
      return str.replaceAll("original", "updated");
    }

    @Override
    public Object evaluateExpression(String str) {
      return context != null && context.containsKey(str) ? context.get(str) : str;
    }

    @Override
    public boolean hasVariables(String str) {
      if (context == null) {
        return false;
      }
      if ("random".equals(str)) {
        return true;
      }

      for (Map.Entry<String, Object> entry : context.entrySet()) {
        if (str.equals(entry.getKey())) {
          return true;
        }
      }
      return false;
    }

    @Override
    public ResolveObjectResponse processObject(Object o) {
      if (!(o instanceof ParameterField)) {
        return new ResolveObjectResponse(false, false);
      }

      ParameterField parameterField = (ParameterField) o;
      boolean updated = parameterField.process(this);
      return new ResolveObjectResponse(true, updated);
    }
  }

  @Data
  @Builder
  private static class DummyB {
    ParameterField<DummyC> dummyC1;
    DummyC dummyC2;
    List<Pair<String, String>> pairs;
    Map<String, Object> map;
    Set<String> set;
    int intVal;
    String strVal;
    @NotExpression String strValIgnored;
    Object obj;
    String[][] strArrArr;
  }

  @Value
  @Builder
  private static class DummyC {
    ParameterField<DummyC> dummyC;
    ParameterField<String> strVal1;
    String strVal2;
    ParameterField<Integer> intVal1;
    int intVal2;
  }
}
