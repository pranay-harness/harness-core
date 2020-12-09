package io.harness.pms.serializer.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.multiline.MultilineStringMixin;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.validation.InputSetValidatorType;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ParameterFieldDeserializerTest extends CategoryTest implements MultilineStringMixin {
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper(new YAMLFactory());
    objectMapper.registerModule(new NGHarnessJacksonModule());
  }

  @Test
  @Owner(developers = OwnerRule.HARSH)
  @Category(UnitTests.class)
  public void testParameterFieldDeserialization() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline.yml");
    Pipeline readValue = objectMapper.readValue(testFile, Pipeline.class);
    assertThat(readValue).isNotNull();
    assertThat(readValue.infrastructure.getValue()).isNotNull();
    assertThat(readValue.infrastructure.getValue().inner1.getValue()).isEqualTo("kubernetes-direct");
    assertThat(readValue.infrastructure.isExpression()).isEqualTo(false);
    assertThat(readValue.infrastructure.getValue().inner2.isExpression()).isEqualTo(true);
    assertThat(readValue.infrastructure.getValue().inner2.getExpressionValue()).isEqualTo("${abc}");
  }

  @Test
  @Owner(developers = OwnerRule.ARCHIT)
  @Category(UnitTests.class)
  public void testParameterFieldInputSetDeserialization() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline.yml");
    Pipeline readValue = objectMapper.readValue(testFile, Pipeline.class);
    assertThat(readValue).isNotNull();
    assertThat(readValue.infrastructure).isNotNull();
    assertThat(readValue.infrastructure.isExpression()).isEqualTo(false);
    assertThat(readValue.infrastructure.getValue()).isNotNull();
    assertThat(readValue.infrastructure.getValue().inner3.isExpression()).isTrue();
    assertThat(readValue.infrastructure.getValue().inner3.getExpressionValue()).isEqualTo("${input}");
    assertThat(readValue.infrastructure.getValue().inner3.getInputSetValidator()).isNull();

    assertThat(readValue.infrastructure.getValue().inner4.isExpression()).isTrue();
    assertThat(readValue.infrastructure.getValue().inner4.getExpressionValue()).isEqualTo("${input}");
    assertThat(readValue.infrastructure.getValue().inner4.getInputSetValidator().getParameters())
        .isEqualTo("dev, ${env}, ${env2}, stage");

    assertThat(readValue.infrastructure.getValue().inner5.isExpression()).isTrue();
    assertThat(readValue.infrastructure.getValue().inner5.getExpressionValue()).isEqualTo("${input}");
    assertThat(readValue.infrastructure.getValue().inner5.getInputSetValidator().getParameters())
        .isEqualTo("jexl(${env} == 'prod' ? 'dev, qa':'prod, stage')");

    assertThat(readValue.infrastructure.getValue().inner6.isExpression()).isTrue();
    assertThat(readValue.infrastructure.getValue().inner6.getExpressionValue()).isEqualTo("${input}");
    assertThat(readValue.infrastructure.getValue().inner6.getInputSetValidator().getParameters()).isEqualTo("^prod*");
    assertThat(readValue.infrastructure.getValue().inner6.getInputSetValidator().getValidatorType())
        .isEqualTo(InputSetValidatorType.REGEX);

    assertThat(readValue.infrastructure.getValue().inner8.isExpression()).isTrue();
    assertThat(readValue.infrastructure.getValue().inner8.getExpressionValue()).isEqualTo("${input}");
    assertThat(readValue.infrastructure.getValue().inner8.getInputSetValidator().getParameters())
        .isEqualTo("jexl(${env} == 'dev' ? (${team} == 'a' ? 'dev_a, dev_b':'dev_qa, dev_qb'):'prod, stage')");
  }

  @Data
  @Builder
  static class Pipeline {
    private ParameterField<Infrastructure> infrastructure;
  }

  @Data
  @Builder
  static class Infrastructure {
    private ParameterField<String> inner1;
    private ParameterField<List<String>> inner2;
    private ParameterField<String> innerNull;
    private ParameterField<Double> inner3;
    private ParameterField<String> inner4;
    private ParameterField<String> inner5;
    private ParameterField<String> inner6;
    private ParameterField<Double> inner7;
    private ParameterField<String> inner8;
  }
}
