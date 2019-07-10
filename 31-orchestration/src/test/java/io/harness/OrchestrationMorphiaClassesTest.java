package io.harness;

import io.harness.category.element.UnitTests;
import io.harness.reflection.CodeUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OrchestrationMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testPackage() {
    CodeUtils.checkHarnessClassBelongToModule(
        CodeUtils.location(OrchestrationMorphiaClasses.class), OrchestrationMorphiaClasses.classes);
  }
}
