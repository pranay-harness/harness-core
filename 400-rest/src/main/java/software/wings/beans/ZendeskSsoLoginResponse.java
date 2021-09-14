/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 11/30/17.
 */
@Data
@Builder
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class ZendeskSsoLoginResponse {
  private String redirectUrl;
  private String userId;
}
