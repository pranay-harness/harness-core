package io.harness.serializer.kryo;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.rule.Owner;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;
import software.wings.WingsBaseTest;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ManagerKryoRegistrarTest extends WingsBaseTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testManagerClasses() {
    Reflections reflections = new Reflections("software.wings", "io.harness");

    Set<Class> result = new HashSet<>();

    Set<Class<? extends ResponseData>> types = reflections.getSubTypesOf(ResponseData.class);
    for (Class type : types) {
      assertThat(KryoUtils.isRegistered(type)).as(type.getName()).isTrue();
    }
  }
}
