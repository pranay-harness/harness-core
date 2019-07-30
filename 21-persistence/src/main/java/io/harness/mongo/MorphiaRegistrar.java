package io.harness.mongo;

import java.util.Map;

public interface MorphiaRegistrar {
  String pkgWings = "software.wings.";
  String pkgHarness = "io.harness.";

  void register(Map<String, Class> map);
}
