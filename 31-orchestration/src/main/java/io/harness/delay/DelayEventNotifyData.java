package io.harness.delay;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class DelayEventNotifyData implements DelegateResponseData {
  private Map<String, String> context;

  public DelayEventNotifyData(Map<String, String> context) {
    this.context = context;
  }
}
