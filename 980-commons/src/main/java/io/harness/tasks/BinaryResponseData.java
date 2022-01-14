package io.harness.tasks;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public class BinaryResponseData implements ResponseData, ProgressData {
  byte[] data;
}
