package io.harness.mongo;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.reflection.CodeUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PersistenceMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testPackage() {
    CodeUtils.checkHarnessClassBelongToModule(
        CodeUtils.location(PersistenceMorphiaClasses.class), PersistenceMorphiaClasses.classes);
  }
}
