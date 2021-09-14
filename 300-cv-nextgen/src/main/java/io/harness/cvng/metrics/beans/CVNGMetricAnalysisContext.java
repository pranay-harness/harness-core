/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.metrics.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CVNGMetricAnalysisContext extends AccountMetricContext {
  public CVNGMetricAnalysisContext(String accountId, String verificationTaskId) {
    super(accountId);
    put("verificationTaskId", verificationTaskId);
  }
}
