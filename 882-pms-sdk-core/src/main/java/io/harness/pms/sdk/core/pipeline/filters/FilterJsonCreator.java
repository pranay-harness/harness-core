package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;

import java.util.Map;
import java.util.Set;

public interface FilterJsonCreator<T> {
  Class<T> getFieldClass();
  Map<String, Set<String>> getSupportedTypes();
  FilterCreationResponse handleNode(FilterCreationContext filterCreationContext, T yamlField);
}
