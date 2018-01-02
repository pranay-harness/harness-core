package software.wings.utils;

import org.junit.Test;

import java.util.Map;

public class SerializationClassesTest {
  @Test
  public void serializationClassesShouldExist() throws ClassNotFoundException {
    Map<String, Integer> classIds = SerializationClasses.serializationClasses();
    for (String s : classIds.keySet()) {
      Class.forName(s);
    }
  }
}
