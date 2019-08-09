package io.harness.morphia;

import java.util.Map;
import java.util.Set;

public interface MorphiaRegistrar {
  String pkgWings = "software.wings.";
  String pkgHarness = "io.harness.";

  interface HelperPut {
    void put(String path, Class clazz);
  }

  void registerClasses(Set<Class> set);

  void registerImplementationClasses(Map<String, Class> map);
}
