package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.SerializationClasses.serializationClasses;

import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SerializationClassesTest {
  @Test
  public void serializationClassesShouldExist() throws ClassNotFoundException {
    Map<String, Integer> classIds = serializationClasses();
    for (String s : classIds.keySet()) {
      Class.forName(s);
    }
  }

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
