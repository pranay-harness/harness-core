package io.harness.ng.core.artifacts.resources.util;

import io.harness.common.NGExpressionUtils;
import io.harness.data.structure.EmptyPredicate;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ArtifactResourceUtils {
  // Checks whether field is fixed value or not, if empty then also we return false for fixed value.
  public boolean isFieldFixedValue(String fieldValue) {
    return !EmptyPredicate.isEmpty(fieldValue) && !NGExpressionUtils.isRuntimeOrExpressionField(fieldValue);
  }
}
