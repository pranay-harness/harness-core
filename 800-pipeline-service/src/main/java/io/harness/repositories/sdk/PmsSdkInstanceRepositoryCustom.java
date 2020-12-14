package io.harness.repositories.sdk;

import io.harness.pms.contracts.steps.StepInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PmsSdkInstanceRepositoryCustom {
  void updatePmsSdkInstance(String name, Map<String, Set<String>> supportedTypes, List<StepInfo> supportedSteps);
}
