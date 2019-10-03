package io.harness.morphia;

import java.util.Map;
import java.util.Set;

public interface MorphiaRegistrar {
  String PKG_WINGS = "software.wings.";
  String PKG_HARNESS = "io.harness.";

  interface HelperPut {
    void put(String path, Class clazz);
  }

  void registerClasses(Set<Class> set);

  void registerImplementationClasses(Map<String, Class> map);
}
