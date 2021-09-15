/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PL)
public class AccessControlClientConfiguration {
  private boolean enableAccessControl;
  private ServiceHttpClientConfig accessControlServiceConfig;
  private String accessControlServiceSecret;
}
