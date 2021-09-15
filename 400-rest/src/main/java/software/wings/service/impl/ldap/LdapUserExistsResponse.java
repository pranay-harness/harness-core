/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.ldap;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapUserExistsResponse extends AbstractLdapResponse {
  String Dn;

  public LdapUserExistsResponse(@NotNull LdapUserConfig ldapUserConfig, @NotNull LdapResponse ldapResponse, String Dn) {
    super(ldapUserConfig, ldapResponse);
    this.Dn = Dn;
  }
}
