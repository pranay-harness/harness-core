package io.harness.ngpipeline.expressions;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;

import io.harness.NGPipelineTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.expressions.AmbianceExpressionEvaluator;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.EngineJexlContext;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.serializer.jackson.NGHarnessJacksonModule;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.validation.InputSetValidatorFactory;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class ParameterFieldExpressionEvaluatorTest extends NGPipelineTestBase {
  @Mock private PlanExecutionService planExecutionService;
  @Inject private InputSetValidatorFactory inputSetValidatorFactory;

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    when(planExecutionService.get("PLAN_ID")).thenReturn(null);
    objectMapper = new ObjectMapper(new YAMLFactory());
    objectMapper.registerModule(new NGHarnessJacksonModule());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testParameterFieldProcessor() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("parameterField.yml");
    Pipeline pipeline = objectMapper.readValue(testFile, Pipeline.class);

    // Kind of applying input sets on given pipeline
    Infrastructure infrastructure = pipeline.getInfrastructure();
    infrastructure.getInner1().updateWithValue("<+inner02>");
    infrastructure.getInner2().updateWithValue(Arrays.asList("value1", "value2"));
    infrastructure.getInner3().updateWithExpression("<+inner03>");
    infrastructure.getInner4().updateWithValue(Collections.singleton(2));
    Definition definition = infrastructure.getDefinition();
    definition.getInner5().updateWithValue("string1");
    definition.getInner6().updateWithExpression("<+infrastructure.inner3>");
    definition.getInner8().updateWithValue("dev_a");

    EngineExpressionEvaluator evaluator =
        prepareEngineExpressionEvaluator(new ImmutableMap.Builder<String, Object>().put("obj", pipeline).build());

    validateExpression(evaluator, "inner02", "stringval", false);
    validateExpression(evaluator, "infrastructure.inner3", 4.2, true);
    validateExpression(evaluator, "infrastructure.definition.inner7", "stringval", true);
    validateExpression(evaluator, "infrastructure.inner3", 4.2, true);
  }

  private void validateExpression(
      EngineExpressionEvaluator evaluator, String expression, Object expected, boolean skipEvaluate) {
    expression = "<+" + expression + ">";
    assertThat(evaluator.renderExpression(expression)).isEqualTo(String.valueOf(expected));
    if (!skipEvaluate) {
      assertThat(evaluator.evaluateExpression(expression)).isEqualTo(expected);
    }
  }

  // Public classes are needed for Jexl expression evaluation.
  @Data
  @Builder
  public static class Pipeline {
    private Infrastructure infrastructure;
    private ParameterField<List<String>> inner01;
    private ParameterField<String> inner02;
    private ParameterField<Double> inner03;
    private ParameterField<Integer> inner04;
    private ParameterField<List<Double>> inner05;
  }

  @Data
  @Builder
  public static class Infrastructure {
    private ParameterField<String> inner1;
    private ParameterField<List<String>> inner2;
    private ParameterField<Double> inner3;
    private ParameterField<List<Double>> inner4;
    private Definition definition;
  }

  @Data
  public static class Definition {
    private ParameterField<String> inner5;
    private ParameterField<Double> inner6;
    private ParameterField<String> inner7;
    private ParameterField<String> inner8;
  }

  private EngineExpressionEvaluator prepareEngineExpressionEvaluator(Map<String, Object> contextMap) {
    SampleEngineExpressionEvaluator evaluator = new SampleEngineExpressionEvaluator(contextMap);
    on(evaluator).set("planExecutionService", planExecutionService);
    on(evaluator).set("inputSetValidatorFactory", inputSetValidatorFactory);
    return evaluator;
  }

  private static class SampleEngineExpressionEvaluator extends AmbianceExpressionEvaluator {
    Map<String, Object> contextMap;

    SampleEngineExpressionEvaluator(Map<String, Object> contextMap) {
      super(null, Ambiance.newBuilder().setPlanExecutionId("PLAN_ID").build(), null, false);
      this.contextMap = contextMap;
    }

    @Override
    protected void initialize() {
      super.initialize();
      for (Map.Entry<String, Object> entry : contextMap.entrySet()) {
        addToContext(entry.getKey(), entry.getValue());
      }
    }

    @NotNull
    protected List<String> fetchPrefixes() {
      return ImmutableList.of("obj", "");
    }

    @Override
    protected Object evaluateInternal(String expression, EngineJexlContext ctx) {
      Object value = super.evaluateInternal(expression, ctx);
      if (value instanceof ParameterField) {
        return ((ParameterField) value).fetchFinalValue();
      }
      return value;
    }
  }
}
