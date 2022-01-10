/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
