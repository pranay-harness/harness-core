package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DelegateStringResponseData implements DelegateResponseData {
  String data;
}
