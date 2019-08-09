package io.harness.mongo;

import java.util.Map;
import java.util.Set;

public interface MorphiaRegistrar {
  String pkgWings = "software.wings.";
  String pkgHarness = "io.harness.";

  interface HelperPut {
    void put(String path, Class clazz);
  }

  void register(Set<Class> set);

  void register(Map<String, Class> map);
}
