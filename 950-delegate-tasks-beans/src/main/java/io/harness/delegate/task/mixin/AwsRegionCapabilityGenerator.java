/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.AwsRegionCapability;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class AwsRegionCapabilityGenerator {
  public static AwsRegionCapability buildAwsRegionCapability(@NonNull String region) {
    return AwsRegionCapability.builder().region(region).build();
  }
}
