package software.wings.waitnotify;

import io.harness.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorNotifyResponseData implements ResponseData {
  private String errorMessage;
}
