/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.ldap;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.TargetModule;

// Could not move it because this class is used in watcher
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public interface LdapConnectionConfig {
  int getConnectTimeout();

  int getResponseTimeout();

  boolean isSslEnabled();

  boolean isReferralsEnabled();

  int getMaxReferralHops();

  String getBindDN();

  String getBindPassword();

  String generateUrl();
}
