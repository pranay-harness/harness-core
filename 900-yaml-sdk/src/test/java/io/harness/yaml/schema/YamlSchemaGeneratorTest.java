package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.schema.beans.YamlSchemaConfiguration;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
    yamlSchemaGenerator = Mockito.spy(new YamlSchemaGenerator());
    Set<Class<?>> schemaClasses = new HashSet<>();
    schemaClasses.add(TestClass.ClassWhichContainsInterface.class);
    final YamlSchemaConfiguration yamlSchemaConfiguration =
        YamlSchemaConfiguration.builder().generatedPathRoot("testBasePath").build();
    doReturn(schemaClasses).when(yamlSchemaGenerator).getClassesForYamlSchemaGeneration(yamlSchemaConfiguration);
    doNothing().when(yamlSchemaGenerator).writeContentsToFile(any(), any(), any());
    yamlSchemaGenerator.generateYamlSchemaFiles(yamlSchemaConfiguration);

    ArgumentCaptor<String> fileNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> contentArgumentCaptor = ArgumentCaptor.forClass(Object.class);

    verify(yamlSchemaGenerator, times(5))
        .writeContentsToFile(fileNameArgumentCaptor.capture(), contentArgumentCaptor.capture(), any());
    Set<String> filePaths = new HashSet<>();
    filePaths.addAll(Arrays.asList("testBasePath/ClassWhichContainsInterface/ClassWhichContainsInterface.json",
        "testBasePath/ClassWhichContainsInterface/ClassWithoutApiModelOverride.json",
        "testBasePath/ClassWhichContainsInterface/TestInterface.json",
        "testBasePath/ClassWhichContainsInterface/testName.json", "testBasePath/ClassWhichContainsInterface/all.json"));

    assertThat(fileNameArgumentCaptor.getAllValues().size()).isEqualTo(5);
    fileNameArgumentCaptor.getAllValues().forEach(filePath -> { assertThat(filePath).isIn(filePaths); });
  }
}