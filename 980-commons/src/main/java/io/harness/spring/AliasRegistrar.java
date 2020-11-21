package io.harness.spring;

import java.util.Map;

public interface AliasRegistrar {
  void register(Map<String, Class<?>> orchestrationElements);
}
