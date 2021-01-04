package io.harness.yaml.utils;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.schema.TestClass;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class YamlSchemaUtilsTest extends CategoryTest {
  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetSwaggerName() {
    final String swaggerName = YamlSchemaUtils.getSwaggerName(TestClass.ClassWithApiModelOverride.class);
    final String swaggerName1 = YamlSchemaUtils.getSwaggerName(TestClass.ClassWithoutApiModelOverride.class);
    assertThat(swaggerName).isEqualTo("testName");
    assertThat(swaggerName1).isEqualTo("ClassWithoutApiModelOverride");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetSchemaPathForEntityType() {
    final String schemaPath = YamlSchemaUtils.getSchemaPathForEntityType(EntityType.CONNECTORS, "abc");
    assertThat(schemaPath).isEqualTo("abc/" + EntityType.CONNECTORS.getYamlName() + "/all.json");
    final String schemaPath1 = YamlSchemaUtils.getSchemaPathForEntityType(EntityType.CONNECTORS, "");
    assertThat(schemaPath1).isEqualTo(EntityType.CONNECTORS.getYamlName() + "/all.json");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetFieldName() {
    final Field[] declaredFields = TestClass.ClassWithApiModelOverride.class.getDeclaredFields();
    final Set<String> result =
        Arrays.stream(declaredFields).map(YamlSchemaUtils::getFieldName).collect(Collectors.toSet());
    assertThat(result).containsExactlyInAnyOrder("a", "testString", "b", "apimodelproperty", "jsontypeinfo");
  }
}