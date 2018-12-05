package software.wings.waitnotify;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StringNotifyResponseData implements ResponseData {
  private String data;
}
