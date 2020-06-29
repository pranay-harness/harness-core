package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.OrchestrationMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OrchestrationMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testPackage() {
    new OrchestrationMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testOrchestrationImplementationClassesModule() {
    new OrchestrationMorphiaRegistrar().testImplementationClassesModule();
  }
}
