package software.wings.sm;

import lombok.Builder;
import lombok.Value;
import software.wings.waitnotify.NotifyResponseData;

@Value
@Builder
public class BarrierStatusData implements NotifyResponseData {
  boolean failed;
}