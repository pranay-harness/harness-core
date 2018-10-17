package software.wings.service.impl;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class DelayEventNotifyData implements ResponseData {
  private Map<String, String> context;

  public DelayEventNotifyData(Map<String, String> context) {
    this.context = context;
  }
}
