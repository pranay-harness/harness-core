package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.pms.pipeline.filter.FilterCreationResponse;

import java.util.Map;
import java.util.Set;

public interface FilterJsonCreator<T> {
  Class<T> getFieldClass();
  Map<String, Set<String>> getSupportedTypes();
  FilterCreationResponse handleNode(T yamlField);
}
