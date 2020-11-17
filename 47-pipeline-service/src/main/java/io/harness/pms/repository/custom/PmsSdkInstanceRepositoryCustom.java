package io.harness.pms.repository.custom;

import java.util.List;
import java.util.Map;

public interface PmsSdkInstanceRepositoryCustom {
  void updateSupportedTypes(String name, Map<String, List<String>> supportedTypes);
}
