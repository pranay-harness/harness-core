package io.harness.serializer.kryo;

import static io.harness.serializer.kryo.SerializationClasses.serializationClasses;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SerializationClassesTest {
  @Test
  public void serializationClassesUniqueIds() {
    Map<String, Integer> classIds = serializationClasses();
    Set<Integer> ids = new HashSet<>();

    classIds.forEach((cls, id) -> {
      assertThat(ids.contains(id)).as("Class %s has duplicate id %d", cls, id).isFalse();
      ids.add(id);
    });
  }
}