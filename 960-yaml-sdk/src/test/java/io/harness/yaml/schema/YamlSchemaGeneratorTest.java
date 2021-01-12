package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.TestClassWithManyFields;
import io.harness.yaml.schema.beans.YamlSchemaConfiguration;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class YamlSchemaGeneratorTest extends CategoryTest {
  YamlSchemaGenerator yamlSchemaGenerator;

  @Before
  public void setup() {
    SwaggerGenerator swaggerGenerator = new SwaggerGenerator();
    JacksonClassHelper jacksonClassHelper = new JacksonClassHelper();
    yamlSchemaGenerator = Mockito.spy(new YamlSchemaGenerator(jacksonClassHelper, swaggerGenerator));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGenerateYamlSchemaFiles() throws IOException {
    Set<Class<?>> schemaClasses = new HashSet<>();
    schemaClasses.add(TestClass.ClassWhichContainsInterface.class);
    final YamlSchemaConfiguration yamlSchemaConfiguration =
        YamlSchemaConfiguration.builder().generatedPathRoot("testBasePath").build();
    doReturn(schemaClasses).when(yamlSchemaGenerator).getClassesForYamlSchemaGeneration(yamlSchemaConfiguration);
    doNothing().when(yamlSchemaGenerator).writeContentsToFile(any(), any(), any());
    doReturn("base").when(yamlSchemaGenerator).getPathToStoreSchema(any(), any(), anyString());
    yamlSchemaGenerator.generateYamlSchemaFiles(yamlSchemaConfiguration);

    ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> contentArgumentCaptor = ArgumentCaptor.forClass(Object.class);

    verify(yamlSchemaGenerator, times(5))
        .writeContentsToFile(fileNameArgumentCaptor.capture(), contentArgumentCaptor.capture(), any());

    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testOutputSchema.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    final Object result = contentArgumentCaptor.getAllValues().get(0);
    ObjectMapper mapper = new ObjectMapper();
    assertThat(mapper.readTree(result.toString())).isEqualTo(mapper.readTree(expectedOutput));
  }

  @Test
  @Owner(developers = ABHINAV, intermittent = true)
  @Category({UnitTests.class})
  public void testGenerateYamlSchemaFilesWithFieldHavingManyPossibleValue() throws IOException {
    Set<Class<?>> schemaClasses = new HashSet<>();
    schemaClasses.add(TestClassWithManyFields.ClassWhichContainsInterface1.class);
    final YamlSchemaConfiguration yamlSchemaConfiguration =
        YamlSchemaConfiguration.builder().generatedPathRoot("testBasePath").build();
    doReturn(schemaClasses).when(yamlSchemaGenerator).getClassesForYamlSchemaGeneration(yamlSchemaConfiguration);
    doNothing().when(yamlSchemaGenerator).writeContentsToFile(any(), any(), any());
    doReturn("base1").when(yamlSchemaGenerator).getPathToStoreSchema(any(), any(), anyString());
    yamlSchemaGenerator.generateYamlSchemaFiles(yamlSchemaConfiguration);

    ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> contentArgumentCaptor = ArgumentCaptor.forClass(Object.class);

    verify(yamlSchemaGenerator, times(6))
        .writeContentsToFile(fileNameArgumentCaptor.capture(), contentArgumentCaptor.capture(), any());

    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testOutputSchemaWithManyFields.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    ObjectMapper mapper = new ObjectMapper();
    DefaultPrettyPrinter defaultPrettyPrinter = new SchemaGeneratorUtils.SchemaPrinter();
    ObjectWriter jsonWriter = mapper.writer(defaultPrettyPrinter);
    final String s = jsonWriter.writeValueAsString(contentArgumentCaptor.getAllValues().get(4));
    assertThat(s).isEqualTo(expectedOutput);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGenerateYamlSchemaFilesWithInternalValues() throws IOException {
    Set<Class<?>> schemaClasses = new HashSet<>();
    schemaClasses.add(TestClass.ClassWhichContainsInterfaceWithInternal.class);
    final YamlSchemaConfiguration yamlSchemaConfiguration =
        YamlSchemaConfiguration.builder().generatedPathRoot("testBasePath").build();
    doReturn(schemaClasses).when(yamlSchemaGenerator).getClassesForYamlSchemaGeneration(yamlSchemaConfiguration);
    doNothing().when(yamlSchemaGenerator).writeContentsToFile(any(), any(), any());
    doReturn("base2").when(yamlSchemaGenerator).getPathToStoreSchema(any(), any(), anyString());
    yamlSchemaGenerator.generateYamlSchemaFiles(yamlSchemaConfiguration);

    ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> contentArgumentCaptor = ArgumentCaptor.forClass(Object.class);

    verify(yamlSchemaGenerator, times(5))
        .writeContentsToFile(fileNameArgumentCaptor.capture(), contentArgumentCaptor.capture(), any());

    final String expectedOutput = IOUtils.resourceToString(
        "testSchema/testJsonWithInternalSubtype.json", StandardCharsets.UTF_8, this.getClass().getClassLoader());
    ObjectMapper mapper = new ObjectMapper();
    DefaultPrettyPrinter defaultPrettyPrinter = new SchemaGeneratorUtils.SchemaPrinter();
    ObjectWriter jsonWriter = mapper.writer(defaultPrettyPrinter);
    final String s = jsonWriter.writeValueAsString(contentArgumentCaptor.getAllValues().get(3));
    assertThat(s).isEqualTo(expectedOutput);
  }
}