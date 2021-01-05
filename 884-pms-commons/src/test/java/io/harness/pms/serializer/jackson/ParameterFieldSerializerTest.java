package io.harness.pms.serializer.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.multiline.MultilineStringMixin;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.pms.yaml.validation.InputSetValidatorType;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ParameterFieldSerializerTest extends CategoryTest implements MultilineStringMixin {
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper(new YAMLFactory());
    objectMapper.registerModule(new NGHarnessJacksonModule());
  }

  @Test
  @Owner(developers = OwnerRule.GARVIT)
  @Category(UnitTests.class)
  public void testParameterFieldSerialization() throws IOException {
    SampleParams params = SampleParams.builder().build();
    ParameterField<String> field = ParameterField.createExpressionField(
        true, "<+input>", new InputSetValidator(InputSetValidatorType.ALLOWED_VALUES, "a,b,c"), true);
    params.setInner(field);
    validateSerialization(params);

    field = ParameterField.createValueField("hello");
    params.setInner(field);
    validateSerialization(params);
  }

  private void validateSerialization(SampleParams params) throws IOException {
    String str = objectMapper.writeValueAsString(params);
    SampleParams outParams = objectMapper.readValue(str, SampleParams.class);
    assertThat(outParams).isNotNull();
    if (params.inner == null) {
      assertThat(outParams.inner).isNull();
      return;
    }

    ParameterField<String> in = params.inner;
    ParameterField<String> out = params.inner;
    assertThat(out).isNotNull();
    assertThat(out.isExpression()).isEqualTo(in.isExpression());
    assertThat(out.getExpressionValue()).isEqualTo(in.getExpressionValue());
    assertThat(out.isTypeString()).isEqualTo(in.isTypeString());
    assertThat(out.getValue()).isEqualTo(in.getValue());

    if (in.getInputSetValidator() == null) {
      assertThat(out.getInputSetValidator()).isNull();
      return;
    }

    assertThat(out.getInputSetValidator()).isNotNull();
    assertThat(out.getInputSetValidator().getValidatorType()).isEqualTo(in.getInputSetValidator().getValidatorType());
    assertThat(out.getInputSetValidator().getParameters()).isEqualTo(in.getInputSetValidator().getParameters());
  }

  @Data
  @Builder
  private static class SampleParams {
    ParameterField<String> inner;
  }
}
