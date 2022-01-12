/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class TwoFactorAuthenticationSettings {
  private String userId;
  private String email;
  private boolean twoFactorAuthenticationEnabled;
  private TwoFactorAuthenticationMechanism mechanism;
  private String totpSecretKey;
  private String totpqrurl;
}
