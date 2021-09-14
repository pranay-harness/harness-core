/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.verification;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceGuardThroughputToErrorsMap {
  @Nullable private String txnName;
  private String throughputMetric;
  private List<String> errorMetrics;

  public Map<String, List<String>> getThroughputToErrorsMap() {
    return Collections.singletonMap(throughputMetric, errorMetrics);
  }
}
