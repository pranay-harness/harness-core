package io.harness.repositories.sdk;

import io.harness.pms.contracts.plan.InitializeSdkRequest;

import java.util.Map;
import java.util.Set;

public interface PmsSdkInstanceRepositoryCustom {
  void updatePmsSdkInstance(InitializeSdkRequest request, Map<String, Set<String>> supportedTypes);
}
