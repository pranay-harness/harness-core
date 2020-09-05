package io.harness.waiter;

import io.harness.delegate.beans.DelegateResponseData;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StringNotifyResponseData implements DelegateResponseData {
  private String data;
}
