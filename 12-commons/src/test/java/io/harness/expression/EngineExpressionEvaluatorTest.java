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

    validateSingleExpression(evaluator, "bVal1CVal1.strVal", "c11", false);
    validateExpression(evaluator, "bVal1.cVal1.strVal", "c11");
    validateExpression(evaluator, "bVal1.cVal2.strVal", "finalC12", true);
    validateExpression(evaluator, "bVal1.strVal1", "b11");
    validateExpression(evaluator, "bVal1.strVal2", "b12");
    validateExpression(evaluator, "bVal1.intVal1", 11);
    validateExpression(evaluator, "bVal1.intVal2", 12);
    validateExpression(evaluator, "bVal2.cVal1.strVal", "c21");
    validateExpression(evaluator, "bVal2.cVal2.strVal", "finalC22", true);
    validateExpression(evaluator, "bVal2.strVal1", "finalB21", true);
    validateExpression(evaluator, "bVal2.strVal2", "${b22}");
    validateExpression(evaluator, "bVal2.intVal1", 21);
    validateExpression(evaluator, "bVal2.intVal2", 222, true);
    validateExpression(evaluator, "strVal1", "a1");
    validateExpression(evaluator, "strVal2", "a2");
  }

  private void validateExpression(EngineExpressionEvaluator evaluator, String expression, Object expected) {
    validateExpression(evaluator, expression, expected, false);
  }

  private void validateExpression(
      EngineExpressionEvaluator evaluator, String expression, Object expected, boolean skipEvaluate) {
    validateSingleExpression(evaluator, expression, expected, skipEvaluate);
    validateSingleExpression(evaluator, "obj." + expression, expected, skipEvaluate);
  }

  private void validateSingleExpression(
      EngineExpressionEvaluator evaluator, String expression, Object expected, boolean skipEvaluate) {
    expression = "${" + expression + "}";
    assertThat(evaluator.renderExpression(expression)).isEqualTo(String.valueOf(expected));
    if (!skipEvaluate) {
      assertThat(evaluator.evaluateExpression(expression)).isEqualTo(expected);
    }
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testHasVariables() {
    assertThat(EngineExpressionEvaluator.hasVariables(null)).isFalse();
    assertThat(EngineExpressionEvaluator.hasVariables("abc")).isFalse();
    assertThat(EngineExpressionEvaluator.hasVariables("abc ${")).isFalse();
    assertThat(EngineExpressionEvaluator.hasVariables("abc ${}")).isTrue();
    assertThat(EngineExpressionEvaluator.hasVariables("abc ${ab}")).isTrue();
    assertThat(EngineExpressionEvaluator.hasVariables("abc ${ab} ${cd}")).isTrue();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFindVariables() {
    assertThat(EngineExpressionEvaluator.findVariables(null)).isEmpty();
    assertThat(EngineExpressionEvaluator.findVariables("abc")).isEmpty();
    assertThat(EngineExpressionEvaluator.findVariables("abc ${")).isEmpty();
    assertThat(EngineExpressionEvaluator.findVariables("abc ${}")).containsExactly("${}");
    assertThat(EngineExpressionEvaluator.findVariables("abc ${ab}")).containsExactly("${ab}");
    assertThat(EngineExpressionEvaluator.findVariables("abc ${ab} ${cd}")).containsExactly("${ab}", "${cd}");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testHasSecretVariables() {
    assertThat(EngineExpressionEvaluator.hasSecretVariables(null)).isFalse();
    assertThat(EngineExpressionEvaluator.hasSecretVariables("abc")).isFalse();
    assertThat(EngineExpressionEvaluator.hasSecretVariables("abc ${")).isFalse();
    assertThat(EngineExpressionEvaluator.hasSecretVariables("abc ${}")).isFalse();
    assertThat(EngineExpressionEvaluator.hasSecretVariables("abc ${ab}")).isFalse();
    assertThat(EngineExpressionEvaluator.hasSecretVariables("abc ${secretManager.ab} ${cd}")).isTrue();
    assertThat(EngineExpressionEvaluator.hasSecretVariables("abc ${ab} ${secretManager.cd}")).isTrue();
    assertThat(EngineExpressionEvaluator.hasSecretVariables("abc ${secretManager.ab} ${secretManager.cd}")).isTrue();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFindSecretVariables() {
    assertThat(EngineExpressionEvaluator.findSecretVariables(null)).isEmpty();
    assertThat(EngineExpressionEvaluator.findSecretVariables("abc")).isEmpty();
    assertThat(EngineExpressionEvaluator.findSecretVariables("abc ${")).isEmpty();
    assertThat(EngineExpressionEvaluator.findSecretVariables("abc ${}")).isEmpty();
    assertThat(EngineExpressionEvaluator.findSecretVariables("abc ${ab}")).isEmpty();
    assertThat(EngineExpressionEvaluator.findSecretVariables("abc ${ab} ${cd}")).isEmpty();
    assertThat(EngineExpressionEvaluator.findSecretVariables("abc ${secretManager.ab} ${cd}"))
        .containsExactly("${secretManager.ab}");
    assertThat(EngineExpressionEvaluator.findSecretVariables("abc ${ab} ${secretManager.cd}"))
        .containsExactly("${secretManager.cd}");
    assertThat(EngineExpressionEvaluator.findSecretVariables("abc ${secretManager.ab} ${secretManager.cd}"))
        .containsExactly("${secretManager.ab}", "${secretManager.cd}");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testValidVariableFieldName() {
    assertThat(EngineExpressionEvaluator.validVariableFieldName(null)).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableFieldName("")).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableFieldName("abc_9")).isTrue();
    assertThat(EngineExpressionEvaluator.validVariableFieldName("__abc_9")).isTrue();
    assertThat(EngineExpressionEvaluator.validVariableFieldName("__abc-9")).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableFieldName("__abc$9")).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableFieldName("__abc{9")).isFalse();
    assertThat(EngineExpressionEvaluator.validVariableFieldName("__abc}9")).isFalse();
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

    @Override
    protected void initialize() {
      super.initialize();
      addStaticAlias("bVal1CVal1", "bVal1.cVal1");
    }

    @NotNull
    protected List<String> fetchPrefixes() {
      return ImmutableList.of("obj", "");
    }
  }
}
