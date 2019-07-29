package io.harness.mongo;

import java.util.Map;

public interface MorphiaRegistrar {
  String wingsPackage = "software.wings.";
  String harnessPackage = "io.harness.";

  void register(Map<String, Class> map);
}
