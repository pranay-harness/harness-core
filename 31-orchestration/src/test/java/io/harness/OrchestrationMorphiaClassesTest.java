package io.harness;

import io.harness.category.element.UnitTests;
import io.harness.mongo.HObjectFactory.NotFoundClass;
import io.harness.reflection.CodeUtils;
import io.harness.serializer.morphia.OrchestrationMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrchestrationMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testPackage() {
    final HashSet<Class> classes = new HashSet<>();
    new OrchestrationMorphiaRegistrar().registerClasses(classes);
    CodeUtils.checkHarnessClassBelongToModule(CodeUtils.location(OrchestrationMorphiaRegistrar.class), classes);
  }

  @Test
  @Category(UnitTests.class)
  public void testManagerImplementationClassesModule() {
    final Map<String, Class> map = new HashMap<>();
    new OrchestrationMorphiaRegistrar().registerImplementationClasses(map);

    Set<Class> classes = new HashSet<>(map.values());
    classes.remove(NotFoundClass.class);

    CodeUtils.checkHarnessClassBelongToModule(CodeUtils.location(OrchestrationMorphiaRegistrar.class), classes);
  }
}
