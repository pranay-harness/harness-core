package software.wings.sm;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BarrierStatusData implements ResponseData {
  boolean failed;
}