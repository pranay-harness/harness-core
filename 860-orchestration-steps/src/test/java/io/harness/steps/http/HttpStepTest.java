package io.harness.steps.http;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.http.HttpStepResponse;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
    HttpStepResponse response1 = HttpStepResponse.builder().httpResponseBody(body).build();
    ParameterField<Object> var1 =
        ParameterField.createExpressionField(true, "<+json.object(httpResponseBody).metaData>", null, true);
    ParameterField<Object> var2 =
        ParameterField.createExpressionField(true, "<+json.object(httpResponseBody).notPresent>", null, true);
    ParameterField<Object> var3 = ParameterField.createExpressionField(true, "<+json.not.a.valid.expr>", null, true);
    ParameterField<Object> var4 = ParameterField.createValueField("directValue");
    Map<String, Object> variables = new LinkedHashMap<>();
    variables.put("name1", var1);
    variables.put("name4", var4);

    Map<String, String> evaluatedVariables = HttpStep.evaluateOutputVariables(variables, response1);
    assertThat(evaluatedVariables).isNotEmpty();
    assertThat(evaluatedVariables.get("name1")).isEqualTo("metadataValue");
    assertThat(evaluatedVariables.containsKey("name4")).isFalse();

    variables.put("name2", var2);
    variables.put("name3", var3);

    HttpStepResponse response2 = HttpStepResponse.builder().httpResponseBody(body).build();
    assertThatThrownBy(() -> HttpStep.evaluateOutputVariables(variables, response2)).isNotNull();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateAssertions() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String httpResponseBodyFile = "httpResponseBody.txt";
    String httpResponseBody = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(httpResponseBodyFile)), StandardCharsets.UTF_8);
    HttpStepResponse response =
        HttpStepResponse.builder().httpResponseBody(httpResponseBody).httpResponseCode(200).build();
    HttpStepParameters stepParameters = HttpStepParameters.infoBuilder().build();

    // no assertion
    boolean assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    // not a valid assertion
    stepParameters.setAssertion(ParameterField.createValueField("<+httpResponseCode> 200"));
    assertThatThrownBy(() -> HttpStep.validateAssertions(response, stepParameters))
        .hasMessage("Assertion provided is not a valid expression");

    // status code assertion
    stepParameters.setAssertion(ParameterField.createValueField("<+httpResponseCode> == 200"));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    stepParameters.setAssertion(ParameterField.createValueField("<+httpResponseCode> > 200"));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isFalse();

    // json.select() assertions
    stepParameters.setAssertion(ParameterField.createValueField(
        "<+json.select(\"support.url\", httpResponseBody)> == \"https://reqres.in/#support-heading\""));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    //    stepParameters.setAssertion(ParameterField.createValueField(
    //            "<+json.select(\"data[0].id\", httpResponseBody)> == 1"));
    //    assertion = HttpStep.validateAssertions(response, stepParameters);
    //    assertThat(assertion).isTrue();

    // json.object() assertions
    stepParameters.setAssertion(ParameterField.createValueField(
        "<+json.object(httpResponseBody).support.url> == \"https://reqres.in/#support-heading\""));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    stepParameters.setAssertion(ParameterField.createValueField("<+json.object(httpResponseBody).data[0].id> == 1"));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    stepParameters.setAssertion(ParameterField.createValueField("<+json.object(httpResponseBody).page> == 1"));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    // json.list() assertions
    stepParameters.setAssertion(
        ParameterField.createValueField("<+json.list(\"data\", httpResponseBody).get(1).name> == \"fuchsia rose\""));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    stepParameters.setAssertion(
        ParameterField.createValueField("<+json.list(\"data\", httpResponseBody).get(5).id> == 5"));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isFalse();

    // null case
    stepParameters.setAssertion(ParameterField.createValueField(null));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    // empty string case
    stepParameters.setAssertion(ParameterField.createValueField("  "));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    // non expression true case
    stepParameters.setAssertion(ParameterField.createValueField("true"));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    // non expression true case
    stepParameters.setAssertion(ParameterField.createValueField("1 == 5"));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isFalse();

    // boolean expression field
    stepParameters.setAssertion(
        ParameterField.createExpressionField(true, "<+json.object(httpResponseBody).page> == 1", null, false));
    assertion = HttpStep.validateAssertions(response, stepParameters);
    assertThat(assertion).isTrue();

    // non boolean expression field
    stepParameters.setAssertion(
        ParameterField.createExpressionField(true, "<+json.object(httpResponseBody).page>", null, false));
    assertThatThrownBy(() -> HttpStep.validateAssertions(response, stepParameters))
        .hasMessage("Assertion provided is not a valid expression");
  }
}
