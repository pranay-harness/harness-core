package io.harness.pms.repository.custom;

import java.util.Map;
import java.util.Set;

public interface PmsSdkInstanceRepositoryCustom {
  void updateSupportedTypes(String name, Map<String, Set<String>> supportedTypes);
}
