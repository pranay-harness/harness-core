package io.harness.expression;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.rule.Owner;
import io.harness.utils.ParameterField;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

public class EngineExpressionEvaluatorTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testWithoutHarnessExpressions() {
    EngineExpressionEvaluator evaluator = prepareEngineExpressionEvaluator(null);
    assertThat(evaluator.renderExpression(null)).isEqualTo(null);
    assertThat(evaluator.renderExpression("")).isEqualTo("");
    assertThat(evaluator.renderExpression("true")).isEqualTo("true");
    assertThat(evaluator.renderExpression("true == false")).isEqualTo("true == false");
    assertThat(evaluator.evaluateExpression(null)).isNull();
    assertThat(evaluator.evaluateExpression("")).isEqualTo(null);
    assertThat(evaluator.evaluateExpression("true")).isEqualTo(true);
    assertThat(evaluator.evaluateExpression("true == false")).isEqualTo(false);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testWithoutExpressions() {
    DummyB dummyB1 = DummyB.builder()
                         .cVal1(DummyC.builder().strVal("c11").build())
                         .cVal2(ParameterField.createField(DummyC.builder().strVal("c12").build()))
                         .strVal1("b11")
                         .strVal2(ParameterField.createField("b12"))
                         .intVal1(11)
                         .intVal2(ParameterField.createField(12))
                         .build();
    DummyB dummyB2 = DummyB.builder()
                         .cVal1(DummyC.builder().strVal("c21").build())
                         .cVal2(ParameterField.createField(DummyC.builder().strVal("c22").build()))
                         .strVal1("b21")
                         .strVal2(ParameterField.createField("b22"))
                         .intVal1(21)
                         .intVal2(ParameterField.createField(22))
                         .build();
    DummyA dummyA = DummyA.builder()
                        .bVal1(dummyB1)
                        .bVal2(ParameterField.createField(dummyB2))
                        .strVal1("a1")
                        .strVal2(ParameterField.createField("a2"))
                        .build();

    EngineExpressionEvaluator evaluator =
        prepareEngineExpressionEvaluator(ImmutableMap.of("obj", ParameterField.createField(dummyA)));

    validateExpression(evaluator, "bVal1.cVal1.strVal", "c11");
    validateExpression(evaluator, "bVal1.cVal2.strVal", "c12");
    validateExpression(evaluator, "bVal1.strVal1", "b11");
    validateExpression(evaluator, "bVal1.strVal2", "b12");
    validateExpression(evaluator, "bVal1.intVal1", 11);
    validateExpression(evaluator, "bVal1.intVal2", 12);
    validateExpression(evaluator, "bVal2.cVal1.strVal", "c21");
    validateExpression(evaluator, "bVal2.cVal2.strVal", "c22");
    validateExpression(evaluator, "bVal2.strVal1", "b21");
    validateExpression(evaluator, "bVal2.strVal2", "b22");
    validateExpression(evaluator, "bVal2.intVal1", 21);
    validateExpression(evaluator, "bVal2.intVal2", 22);
    validateExpression(evaluator, "strVal1", "a1");
    validateExpression(evaluator, "strVal2", "a2");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testWithExpressions() {
    DummyB dummyB1 = DummyB.builder()
                         .cVal1(DummyC.builder().strVal("c11").build())
                         .cVal2(ParameterField.createField(null, true, "${c12}"))
                         .strVal1("b11")
                         .strVal2(ParameterField.createField("b12"))
                         .intVal1(11)
                         .intVal2(ParameterField.createField(12))
                         .build();
    DummyB dummyB2 = DummyB.builder()
                         .cVal1(DummyC.builder().strVal("c21").build())
                         .cVal2(ParameterField.createField(null, true, "${c22}"))
                         .strVal1("${b21}")
                         .strVal2(ParameterField.createField(null, true, "${b22}"))
                         .intVal1(21)
                         .intVal2(ParameterField.createField(null, true, "${i22}"))
                         .build();
    DummyA dummyA = DummyA.builder()
                        .bVal1(dummyB1)
                        .bVal2(ParameterField.createField(dummyB2))
                        .strVal1("a1")
                        .strVal2(ParameterField.createField("a2"))
                        .build();

    EngineExpressionEvaluator evaluator =
        prepareEngineExpressionEvaluator(new ImmutableMap.Builder<String, Object>()
                                             .put("obj", ParameterField.createField(dummyA))
                                             .put("c12", "finalC12")
                                             .put("c22", "finalC22")
                                             .put("b21", "finalB21")
                                             .put("i22", 222)
                                             .build());

    validateExpression(evaluator, "bVal1.cVal1.strVal", "c11");
    validateExpression(evaluator, "bVal1.cVal2.strVal", "finalC12");
    validateExpression(evaluator, "bVal1.strVal1", "b11");
    validateExpression(evaluator, "bVal1.strVal2", "b12");
    validateExpression(evaluator, "bVal1.intVal1", 11);
    validateExpression(evaluator, "bVal1.intVal2", 12);
    validateExpression(evaluator, "bVal2.cVal1.strVal", "c21");
    validateExpression(evaluator, "bVal2.cVal2.strVal", "finalC22");
    validateExpression(evaluator, "bVal2.strVal1", "finalB21");
    validateExpression(evaluator, "bVal2.strVal2", "${b22}");
    validateExpression(evaluator, "bVal2.intVal1", 21);
    validateExpression(evaluator, "bVal2.intVal2", 222);
    validateExpression(evaluator, "strVal1", "a1");
    validateExpression(evaluator, "strVal2", "a2");
  }

  private void validateExpression(EngineExpressionEvaluator evaluator, String expression, Object expected) {
    validateSingleExpression(evaluator, expression, expected);
    validateSingleExpression(evaluator, "obj." + expression, expected);
  }

  private void validateSingleExpression(EngineExpressionEvaluator evaluator, String expression, Object expected) {
    expression = "${" + expression + "}";
    assertThat(evaluator.renderExpression(expression)).isEqualTo(String.valueOf(expected));
    assertThat(evaluator.evaluateExpression(expression)).isEqualTo(expected);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testHasExpressions() {
    assertThat(EngineExpressionEvaluator.hasExpressions(null)).isFalse();
    assertThat(EngineExpressionEvaluator.hasExpressions("abc")).isFalse();
    assertThat(EngineExpressionEvaluator.hasExpressions("abc ${")).isFalse();
    assertThat(EngineExpressionEvaluator.hasExpressions("abc ${}")).isTrue();
    assertThat(EngineExpressionEvaluator.hasExpressions("abc ${ab}")).isTrue();
    assertThat(EngineExpressionEvaluator.hasExpressions("abc ${ab} ${cd}")).isTrue();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFindExpressions() {
    assertThat(EngineExpressionEvaluator.findExpressions(null)).isEmpty();
    assertThat(EngineExpressionEvaluator.findExpressions("abc")).isEmpty();
    assertThat(EngineExpressionEvaluator.findExpressions("abc ${")).isEmpty();
    assertThat(EngineExpressionEvaluator.findExpressions("abc ${}")).containsExactly("${}");
    assertThat(EngineExpressionEvaluator.findExpressions("abc ${ab}")).containsExactly("${ab}");
    assertThat(EngineExpressionEvaluator.findExpressions("abc ${ab} ${cd}")).containsExactly("${ab}", "${cd}");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testValidVariableName() {
    assertThat(EngineExpressionEvaluator.validVariableName(null)).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableName("")).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableName("abc_9")).isTrue();
    assertThat(EngineExpressionEvaluator.validVariableName("__abc_9")).isTrue();
    assertThat(EngineExpressionEvaluator.validVariableName("__abc-9")).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableName("__abc$9")).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableName("__abc{9")).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableName("__abc}9")).isFalse();
  }

  @Value
  @Builder
  public static class DummyA {
    DummyB bVal1;
    ParameterField<DummyB> bVal2;
    String strVal1;
    ParameterField<String> strVal2;
  }

  @Value
  @Builder
  public static class DummyB {
    DummyC cVal1;
    ParameterField<DummyC> cVal2;
    String strVal1;
    ParameterField<String> strVal2;
    int intVal1;
    ParameterField<Integer> intVal2;
  }

  @Value
  @Builder
  public static class DummyC {
    String strVal;
  }

  private static EngineExpressionEvaluator prepareEngineExpressionEvaluator(Map<String, Object> contextMap) {
    SampleEngineExpressionEvaluator evaluator = new SampleEngineExpressionEvaluator();
    if (EmptyPredicate.isEmpty(contextMap)) {
      return evaluator;
    }

    for (Map.Entry<String, Object> entry : contextMap.entrySet()) {
      evaluator.addToContext(entry.getKey(), entry.getValue());
    }
    return evaluator;
  }

  public static class SampleEngineExpressionEvaluator extends EngineExpressionEvaluator {
    public SampleEngineExpressionEvaluator() {
      super(null);
    }

    @NotNull
    protected List<String> fetchPrefixes() {
      return ImmutableList.of("obj", "");
    }
  }
}
