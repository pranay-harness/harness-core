package io.harness.serializer.kryo;

import static io.harness.serializer.kryo.SerializationClasses.serializationClasses;

import io.harness.category.element.UnitTests;
import io.harness.serializer.KryoUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

public class SerializationClassesTest {
  @Test
  @Category(UnitTests.class)
  public void kryoInit() {
    KryoUtils.clone(1);
  }

  @Test
  @Category(UnitTests.class)
  public void serializationClassesShouldExist() throws ClassNotFoundException {
    Map<String, Integer> classIds = serializationClasses();
    for (String s : classIds.keySet()) {
      Class.forName(s);
    }
  }
}
