package io.harness.yaml.validator;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class YamlSchemaValidatorTest extends CategoryTest {
  YamlSchemaValidator yamlSchemaValidator;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidate() throws IOException {
    yamlSchemaValidator = Mockito.spy(new YamlSchemaValidator());
    intializeAndMockInputStream();

    final String type1 = getYamlResource("validator/testyamltype1.yaml");
    final Set<String> type1Val = yamlSchemaValidator.validate(type1, EntityType.CONNECTORS);
    assertThat(type1Val).isEmpty();

    intializeAndMockInputStream();
    final String type2 = getYamlResource("validator/testyamltype2.yaml");
    final Set<String> type2Val = yamlSchemaValidator.validate(type2, EntityType.CONNECTORS);
    assertThat(type2Val).isEmpty();

    intializeAndMockInputStream();
    final String type1Incorrect = getYamlResource("validator/testType1Incorrect.yaml");
    final Set<String> type1IncorrectVal = yamlSchemaValidator.validate(type1Incorrect, EntityType.CONNECTORS);
    assertThat(type1IncorrectVal).isNotEmpty();

    intializeAndMockInputStream();
    final String type2Incorrect = getYamlResource("validator/testYamlType2Incorrect.yaml");
    final Set<String> type2IncorrectVal = yamlSchemaValidator.validate(type2Incorrect, EntityType.CONNECTORS);
    assertThat(type2IncorrectVal).isNotEmpty();
  }

  private void intializeAndMockInputStream() {
    final InputStream resourceAsStream =
        YamlSchemaValidator.class.getClassLoader().getResourceAsStream("validator/schema.json");
    doReturn(resourceAsStream).when(yamlSchemaValidator).getSchemaFromResource(any());
  }

  public String getYamlResource(String resource) throws IOException {
    return IOUtils.resourceToString(resource, StandardCharsets.UTF_8, YamlSchemaValidatorTest.class.getClassLoader());
  }
}