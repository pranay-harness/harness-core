/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.executioncapability;

import io.harness.expression.ExpressionEvaluator;

import java.util.List;

public interface ExecutionCapabilityDemander {
  List<io.harness.delegate.beans.executioncapability.ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator);
}
