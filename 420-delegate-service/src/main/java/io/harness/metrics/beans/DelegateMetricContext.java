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
