package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class YamlSchemaUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetSwaggerName() {
    final String swaggerName = YamlSchemaUtils.getSwaggerName(TestClass.ClassWithApiModelOverride.class);
    final String swaggerName1 = YamlSchemaUtils.getSwaggerName(TestClass.ClassWithoutApiModelOverride.class);
    assertThat(swaggerName).isEqualTo("testName");
    assertThat(swaggerName1).isEqualTo("ClassWithoutApiModelOverride");
  }
}