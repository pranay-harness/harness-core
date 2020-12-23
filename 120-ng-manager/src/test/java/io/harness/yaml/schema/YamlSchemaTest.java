package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.NextGenApplication;
import io.harness.rule.Owner;
import io.harness.yaml.YamlSdkConstants;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class YamlSchemaTest extends CategoryTest implements AbstractSchemaChecker {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSchemasAreUpdated() throws IOException {
    ensureSchemaUpdated();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testOneOfHasCorrectVales() throws NoSuchFieldException {
    ensureOneOfHasCorrectValues();
  }

  @Override
  public ClassLoader getClassLoader() {
    return NextGenApplication.class.getClassLoader();
  }

  @Override
  public String getSchemaBasePath() {
    return YamlSdkConstants.schemaBasePath;
  }
}
