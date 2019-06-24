package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AwsRegionCapabilityGenerator {
  public static AwsRegionCapability buildAwsRegionCapability(@NonNull String region) {
    return AwsRegionCapability.builder().region(region).build();
  }
}
