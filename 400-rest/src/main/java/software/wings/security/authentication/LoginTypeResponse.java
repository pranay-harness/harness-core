/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.ng.core.account.DefaultExperience;

import software.wings.security.saml.SSORequest;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
@Data
@Builder
public class LoginTypeResponse {
  private AuthenticationMechanism authenticationMechanism;
  private SSORequest SSORequest;
  private boolean isOauthEnabled;
  private boolean showCaptcha;
  private DefaultExperience defaultExperience;
}
