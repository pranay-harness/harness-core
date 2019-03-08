package io.harness.waiter;

import io.harness.delegate.beans.ResponseData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StringNotifyResponseData implements ResponseData {
  private String data;
}
