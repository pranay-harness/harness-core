package io.harness.mongo;

import java.util.Map;

public interface MorphiaRegistrar {
  String pkgWings = "software.wings.";
  String pkgHarness = "io.harness.";

  interface HelperPut {
    void put(String path, Class clazz);
  }

  void register(Map<String, Class> map);
}
