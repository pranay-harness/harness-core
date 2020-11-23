package io.harness.pms.sdk.creator.filters;

import io.harness.pms.filter.FilterCreationResponse;

import java.util.Map;
import java.util.Set;

public interface FilterJsonCreator<T> {
  Class<T> getFieldClass();
  Map<String, Set<String>> getSupportedTypes();
  FilterCreationResponse handleNode(T yamlField);
}
