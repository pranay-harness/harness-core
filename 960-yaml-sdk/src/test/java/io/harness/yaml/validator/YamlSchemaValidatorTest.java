package io.harness.yaml.validator;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.YamlSdkConfiguration;
import io.harness.yaml.schema.TestClass;
import io.harness.yaml.utils.YamlSchemaUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({YamlSchemaUtils.class, IOUtils.class})
public class YamlSchemaValidatorTest extends CategoryTest {
  YamlSchemaValidator yamlSchemaValidator;

  @Before
  public void setup() throws IOException {
    initMocks(this);
    yamlSchemaValidator = Mockito.spy(new YamlSchemaValidator());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidate() throws IOException {
    final String type1 = getYamlResource("validator/testyamltype1.yaml");
    final String type2 = getYamlResource("validator/testyamltype2.yaml");
    final String type1Incorrect = getYamlResource("validator/testType1Incorrect.yaml");
    final String type2Incorrect = getYamlResource("validator/testYamlType2Incorrect.yaml");
    intializeAndMockInputStream();
    Set<Class<?>> classes = new HashSet<>();
    classes.add(TestClass.ClassWhichContainsInterface.class);
    yamlSchemaValidator.populateSchemaInStaticMap(YamlSdkConfiguration.schemaBasePath, classes);

    final Set<String> type1Val = yamlSchemaValidator.validate(type1, EntityType.CONNECTORS);
    assertThat(type1Val).isEmpty();

    final Set<String> type2Val = yamlSchemaValidator.validate(type2, EntityType.CONNECTORS);
    assertThat(type2Val).isEmpty();

    final Set<String> type1IncorrectVal = yamlSchemaValidator.validate(type1Incorrect, EntityType.CONNECTORS);
    assertThat(type1IncorrectVal).isNotEmpty();

    final Set<String> type2IncorrectVal = yamlSchemaValidator.validate(type2Incorrect, EntityType.CONNECTORS);
    assertThat(type2IncorrectVal).isNotEmpty();
  }

  private void intializeAndMockInputStream() throws IOException {
    String schema = getYamlResource("validator/schema.json");
    mockStatic(YamlSchemaUtils.class);
    mockStatic(IOUtils.class);
    Set<Class<?>> classes = new HashSet<>();
    classes.add(TestClass.ClassWhichContainsInterface.class);
    when(YamlSchemaUtils.getClasses(any())).thenReturn(classes);
    when(IOUtils.resourceToString(any(), any(), any())).thenReturn(schema);
  }

  private String getYamlResource(String resource) throws IOException {
    return IOUtils.resourceToString(resource, StandardCharsets.UTF_8, YamlSchemaValidatorTest.class.getClassLoader());
  }
}