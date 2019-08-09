package io.harness.serializer.morphia;

import io.harness.category.element.UnitTests;
import io.harness.reflection.CodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.HashSet;

@Slf4j
public class LimitMorphiaRegistrarTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testLimitsModule() {
    final HashSet<Class> classes = new HashSet<>();
    new LimitsMorphiaRegistrar().registerClasses(classes);
    CodeUtils.checkHarnessClassBelongToModule(CodeUtils.location(LimitsMorphiaRegistrar.class), classes);
  }
}
