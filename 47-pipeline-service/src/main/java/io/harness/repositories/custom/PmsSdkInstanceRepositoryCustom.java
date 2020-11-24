package io.harness.repositories.custom;

import java.util.Map;
import java.util.Set;

public interface PmsSdkInstanceRepositoryCustom {
  void updateSupportedTypes(String name, Map<String, Set<String>> supportedTypes);
}
