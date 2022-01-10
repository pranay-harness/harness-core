/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.switchaccount;

import io.harness.ng.core.account.AuthenticationMechanism;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
@AllArgsConstructor
public class RestrictedSwitchAccountInfo {
  boolean skipReAuthentication;
  boolean isHarnessSupportGroupUser;
  boolean isTwoFactorAuthEnabledForAccount;
  AuthenticationMechanism authenticationMechanism;
  LdapIdentificationInfo ldapIdentificationInfo;
  OauthIdentificationInfo oauthIdentificationInfo;
  SamlIdentificationInfo samlIdentificationInfo;
  Set<String> whitelistedDomains;
}
