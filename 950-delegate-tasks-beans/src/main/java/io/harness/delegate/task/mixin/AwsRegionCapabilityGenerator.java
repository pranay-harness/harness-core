/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
