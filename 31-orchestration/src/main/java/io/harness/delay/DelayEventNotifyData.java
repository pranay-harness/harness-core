package io.harness.delay;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class DelayEventNotifyData implements ResponseData {
  private Map<String, String> context;

  public DelayEventNotifyData(Map<String, String> context) {
    this.context = context;
  }
}
