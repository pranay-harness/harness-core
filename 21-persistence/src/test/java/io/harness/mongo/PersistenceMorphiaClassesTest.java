package io.harness.mongo;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.reflection.CodeUtils;
import io.harness.serializer.morphia.PersistenceMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashSet;

public class PersistenceMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testPackage() {
    final HashSet<Class> classes = new HashSet<>();
    new PersistenceMorphiaRegistrar().registerMyClasses(classes);
    CodeUtils.checkHarnessClassBelongToModule(CodeUtils.location(PersistenceMorphiaRegistrar.class), classes);
  }
}