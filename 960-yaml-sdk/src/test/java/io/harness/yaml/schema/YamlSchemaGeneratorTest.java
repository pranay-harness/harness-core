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
import io.harness.yaml.schema.beans.YamlSchemaConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
    SwaggerGenerator swaggerGenerator = new io.harness.yaml.schema.SwaggerGenerator();
    JacksonClassHelper jacksonClassHelper = new JacksonClassHelper();
    yamlSchemaGenerator = Mockito.spy(new YamlSchemaGenerator(jacksonClassHelper, swaggerGenerator));
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
}