package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DelegateStringProgressData implements DelegateProgressData {
  String data;
}
