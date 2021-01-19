package io.harness.cdng.pipeline.steps;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.http.HttpStepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.StringNGVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HttpStepTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testOutputVariablesEvaluation() {
    String body = "{\n"
        + "    \"status\": \"SUCCESS\",\n"
        + "    \"metaData\": \"metadataValue\",\n"
        + "    \"correlationId\": \"333333344444444\"\n"
        + "}";
    HttpStepResponse response = HttpStepResponse.builder().httpResponseBody(body).build();
    NGVariable var1 =
        StringNGVariable.builder()
            .name("name1")
            .value(ParameterField.createExpressionField(true, "<+json.object(httpResponseBody).metaData>", null, true))
            .build();
    NGVariable var2 = StringNGVariable.builder()
                          .name("name2")
                          .value(ParameterField.createExpressionField(
                              true, "<+json.object(httpResponseBody).notPresent>", null, true))
                          .build();
    NGVariable var3 = StringNGVariable.builder()
                          .name("name3")
                          .value(ParameterField.createExpressionField(true, "<+json.not.a.valid.expr>", null, true))
                          .build();
    NGVariable var4 =
        StringNGVariable.builder().name("name4").value(ParameterField.createValueField("directValue")).build();
    List<NGVariable> variables = new ArrayList<>();
    variables.add(var1);
    variables.add(var2);
    variables.add(var3);
    variables.add(var4);
    Map<String, String> evaluatedVariables = HttpStep.evaluateOutputVariables(variables, response);
    assertThat(evaluatedVariables).isNotEmpty();
    assertThat(evaluatedVariables.get("name1")).isEqualTo("metadataValue");
    assertThat(evaluatedVariables.get("name2")).isEqualTo("<+json.object(httpResponseBody).notPresent>");
    assertThat(evaluatedVariables.get("name3")).isEqualTo("<+json.not.a.valid.expr>");
    assertThat(evaluatedVariables.containsKey("name4")).isFalse();
  }
}