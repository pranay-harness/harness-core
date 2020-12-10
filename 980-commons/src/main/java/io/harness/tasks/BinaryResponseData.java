package io.harness.tasks;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BinaryResponseData implements ResponseData {
  byte[] data;
}
