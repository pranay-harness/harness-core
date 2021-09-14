/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.ServiceHttpClientConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PL)
public class AccessControlAdminClientConfiguration {
  ServiceHttpClientConfig accessControlServiceConfig;
  String accessControlServiceSecret;
  Boolean mockAccessControlService;
}
