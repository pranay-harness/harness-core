/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oidc.gcp.constants;

import static io.harness.oidc.gcp.constants.GcpOidcIdTokenConstants.SA_ACCESS_TOKEN;
import static io.harness.oidc.gcp.constants.GcpOidcIdTokenConstants.SA_ACCESS_TOKEN_EXPIRY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(HarnessTeam.PL)
public class GcpOidcServiceAccountAccessTokenResponse {
  @JsonProperty(SA_ACCESS_TOKEN) private String accessToken;
  @JsonProperty(SA_ACCESS_TOKEN_EXPIRY) private String expireTime;
}