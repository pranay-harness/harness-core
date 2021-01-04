package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class YamlSchemaTest extends CategoryTest implements AbstractSchemaChecker {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSchemas() {
    schemaTests(log);
  }
}
