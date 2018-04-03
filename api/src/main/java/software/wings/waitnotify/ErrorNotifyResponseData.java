package software.wings.waitnotify;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorNotifyResponseData implements NotifyResponseData {
  private String errorMessage;
}
