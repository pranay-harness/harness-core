/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.perpetualtask.instancesync;

import io.harness.perpetualtask.PerpetualTaskClientParams;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureVMSSInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  private String appId;
  private String infraMappingId;
  private String vmssId;
}
