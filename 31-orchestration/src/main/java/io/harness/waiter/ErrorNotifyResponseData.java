package io.harness.waiter;

import io.harness.delegate.beans.ResponseData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorNotifyResponseData implements ResponseData {
  private String errorMessage;
}
