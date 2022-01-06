/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.metrics.beans;

import io.harness.metrics.AutoMetricContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class DelegateMetricContext extends AutoMetricContext {
  public DelegateMetricContext(
      String accountId, String delegateId, long lastHeartbeat, boolean ng, String status, String version) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    String lastHeartbeatDateString = simpleDateFormat.format(new Date(lastHeartbeat));
    put("accountId", accountId);
    put("delegateId", delegateId);
    put("lastHeartbeat", lastHeartbeatDateString);
    put("ng", String.valueOf(ng));
    put("status", status);
    put("version", version);
  }

  public DelegateMetricContext(String accountId) {
    put("accountId", accountId);
  }
}
