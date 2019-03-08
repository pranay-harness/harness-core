package software.wings.sm;

import io.harness.delegate.beans.ResponseData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BarrierStatusData implements ResponseData {
  boolean failed;
}