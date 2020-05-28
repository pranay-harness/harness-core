package io.harness.serializer.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.utils.ParameterField;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ParameterFieldDeserializerTest extends CategoryTest implements MultilineStringMixin {
  private ObjectMapper objectMapper;

  @Before
  public void setUp() throws Exception {
    objectMapper = new ObjectMapper(new YAMLFactory());
    objectMapper.registerModule(new HarnessJacksonModule());
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
  }
}
