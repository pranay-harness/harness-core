package software.wings.service.impl.instance;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Status {
  boolean success;
  String errorMessage;
  boolean retryable;
}
