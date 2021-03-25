package io.harness.ng.core.artifacts.resources.util;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.common.NGExpressionUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ArtifactResourceUtils {
  // Checks whether field is fixed value or not, if empty then also we return false for fixed value.
  public boolean isFieldFixedValue(String fieldValue) {
    return !hasNone(fieldValue) && !NGExpressionUtils.isRuntimeOrExpressionField(fieldValue);
  }
}
