package software.wings.service.impl;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class DelayEventNotifyData implements NotifyResponseData {
  private Map<String, String> context;

  public DelayEventNotifyData(Map<String, String> context) {
    this.context = context;
  }
}
