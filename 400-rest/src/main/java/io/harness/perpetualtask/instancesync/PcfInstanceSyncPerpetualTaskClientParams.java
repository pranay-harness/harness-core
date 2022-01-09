/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.perpetualtask.instancesync;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.PerpetualTaskClientParams;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CDP)
public class PcfInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  String accountId;
  String inframappingId;
  String applicationName;
  String appId;
}
