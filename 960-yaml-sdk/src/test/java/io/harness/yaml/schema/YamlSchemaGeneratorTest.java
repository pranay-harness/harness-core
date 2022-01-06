/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.TestClassWithManyFields;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.dropwizard.jackson.Jackson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DX)
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

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGenerateYamlSchemaFilesWithInternalValuesAsList() throws IOException {
    setup(TestClass.ClassWhichContainsInterfaceWithInternalWithList.class);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    assertThat(entityTypeJsonNodeMap.size()).isEqualTo(1);
    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testJsonWithInternalSubtypeList.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    ObjectWriter jsonWriter = yamlSchemaGenerator.getObjectWriter();
    final String s = jsonWriter.writeValueAsString(entityTypeJsonNodeMap.get(EntityType.CONNECTORS));
    assertThat(s).isEqualTo(expectedOutput);
  }

  private void setup(Class<?> clazz) {
    SwaggerGenerator swaggerGenerator = new SwaggerGenerator(Jackson.newObjectMapper());
    JacksonClassHelper jacksonClassHelper = new JacksonClassHelper(Jackson.newObjectMapper());
    final List<YamlSchemaRootClass> yamlSchemaRootClasses =
        Collections.singletonList(YamlSchemaRootClass.builder().entityType(EntityType.CONNECTORS).clazz(clazz).build());

    yamlSchemaGenerator = new YamlSchemaGenerator(jacksonClassHelper, swaggerGenerator, yamlSchemaRootClasses);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({UnitTests.class})
  public void testGenerateYamlSchemaWithPatternAndLength() throws IOException {
    setup(TestClassWithManyFields.ClassWithoutApiModelOverride3.class);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    assertThat(entityTypeJsonNodeMap.size()).isEqualTo(1);
    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testSchema1.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    ObjectWriter jsonWriter = yamlSchemaGenerator.getObjectWriter();
    final String s = jsonWriter.writeValueAsString(entityTypeJsonNodeMap.get(EntityType.CONNECTORS));
    assertThat(s).isEqualTo(expectedOutput);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({UnitTests.class})
  public void testGenerateYamlSchemaWithFakePatternAndLength() throws IOException {
    setup(TestClassWithManyFields.ClassWithoutApiModelOverride4.class);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    assertThat(entityTypeJsonNodeMap.size()).isEqualTo(1);
    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testSchema2.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    ObjectWriter jsonWriter = yamlSchemaGenerator.getObjectWriter();
    final String s = jsonWriter.writeValueAsString(entityTypeJsonNodeMap.get(EntityType.CONNECTORS));
    assertThat(s).isEqualTo(expectedOutput);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({UnitTests.class})
  public void testGenerateYamlSchemaWithNonEmptyField() throws IOException {
    setup(TestClassWithManyFields.ClassWithNonEmptyField.class);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    assertThat(entityTypeJsonNodeMap.size()).isEqualTo(1);
    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testNotEmptyField.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    ObjectWriter jsonWriter = yamlSchemaGenerator.getObjectWriter();
    final String s = jsonWriter.writeValueAsString(entityTypeJsonNodeMap.get(EntityType.CONNECTORS));
    assertThat(s).isEqualTo(expectedOutput);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testGenerateYamlSchemaWithOneOfSetAnnotation() throws IOException {
    setup(TestClass.ClassWithOneOfSetAnnotation.class);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    assertThat(entityTypeJsonNodeMap.size()).isEqualTo(1);
    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testOneOfSetSchema.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    final Object result = entityTypeJsonNodeMap.get(EntityType.CONNECTORS);
    ObjectMapper mapper = new ObjectMapper();
    assertThat(mapper.readTree(result.toString())).isEqualTo(mapper.readTree(expectedOutput));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testGenerateYamlSchemaWithOneOfSetAnnotation_WithInternalValues() throws IOException {
    setup(TestClass.ClassWhichContainsInterfaceWithInternalAndOneOfSetAnnotation.class);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    assertThat(entityTypeJsonNodeMap.size()).isEqualTo(1);
    final String expectedOutput = IOUtils.resourceToString("testSchema/testOneOfSetSchemaWithInternalProperty.json",
        StandardCharsets.UTF_8, this.getClass().getClassLoader());
    final Object result = entityTypeJsonNodeMap.get(EntityType.CONNECTORS);
    ObjectMapper mapper = new ObjectMapper();
    assertThat(mapper.readTree(result.toString())).isEqualTo(mapper.readTree(expectedOutput));
  }
}
