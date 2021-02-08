package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.TestClassWithManyFields;
import io.harness.yaml.schema.beans.YamlSchemaConfiguration;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class YamlSchemaGeneratorTest extends CategoryTest {
  YamlSchemaGenerator yamlSchemaGenerator;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGenerateYamlSchemaFiles() throws IOException {
    setup(TestClass.ClassWhichContainsInterface.class);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    assertThat(entityTypeJsonNodeMap.size()).isEqualTo(1);
    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testOutputSchema.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    final Object result = entityTypeJsonNodeMap.get(EntityType.CONNECTORS);
    ObjectMapper mapper = new ObjectMapper();
    assertThat(mapper.readTree(result.toString())).isEqualTo(mapper.readTree(expectedOutput));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({UnitTests.class})
  public void testGenerateYamlSchemaFilesWithFieldHavingManyPossibleValue() throws IOException {
    setup(TestClassWithManyFields.ClassWhichContainsInterface1.class);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    assertThat(entityTypeJsonNodeMap.size()).isEqualTo(1);
    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testOutputSchemaWithManyFields.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    ObjectWriter jsonWriter = yamlSchemaGenerator.getObjectWriter();
    final String s = jsonWriter.writeValueAsString(entityTypeJsonNodeMap.get(EntityType.CONNECTORS));
    assertThat(s).isEqualTo(expectedOutput);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGenerateYamlSchemaFilesWithInternalValues() throws IOException {
    setup(TestClass.ClassWhichContainsInterfaceWithInternal.class);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    assertThat(entityTypeJsonNodeMap.size()).isEqualTo(1);
    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testJsonWithInternalSubtype.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    ObjectWriter jsonWriter = yamlSchemaGenerator.getObjectWriter();
    final String s = jsonWriter.writeValueAsString(entityTypeJsonNodeMap.get(EntityType.CONNECTORS));
    assertThat(s).isEqualTo(expectedOutput);
  }

  private void setup(Class<?> clazz) {
    SwaggerGenerator swaggerGenerator = new SwaggerGenerator();
    JacksonClassHelper jacksonClassHelper = new JacksonClassHelper();
    final List<YamlSchemaRootClass> yamlSchemaRootClasses = Collections.singletonList(
        (YamlSchemaRootClass.builder().entityType(EntityType.CONNECTORS).clazz(clazz).build()));

    yamlSchemaGenerator = new YamlSchemaGenerator(jacksonClassHelper, swaggerGenerator, yamlSchemaRootClasses);
  }
}