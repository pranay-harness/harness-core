package io.harness;

import io.harness.category.element.UnitTests;
import io.harness.reflection.CodeUtils;
import io.harness.serializer.morphia.OrchestrationMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashSet;

public class OrchestrationMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testPackage() {
    final HashSet<Class> classes = new HashSet<>();
    new OrchestrationMorphiaRegistrar().register(classes);
    CodeUtils.checkHarnessClassBelongToModule(CodeUtils.location(OrchestrationMorphiaRegistrar.class), classes);
  }
}
