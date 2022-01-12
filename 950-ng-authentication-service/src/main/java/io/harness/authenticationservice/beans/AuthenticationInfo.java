/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.authenticationservice.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.ng.core.account.OauthProviderType;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class AuthenticationInfo {
  private AuthenticationMechanism authenticationMechanism;
  private boolean oauthEnabled;
  private List<OauthProviderType> oauthProviders;
  private String samlRedirectUrl;
  private String accountId;
}
