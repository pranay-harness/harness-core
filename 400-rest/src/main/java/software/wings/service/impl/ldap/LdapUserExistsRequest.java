/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.ldap;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.helpers.ext.ldap.LdapSearch;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapUserExistsRequest extends AbstractLdapRequest {
  LdapSearch ldapSearch;
  String identifier;

  public LdapUserExistsRequest(@NotNull final LdapUserConfig ldapUserConfig, @NotNull final LdapSearch ldapSearch,
      @NotNull String identifier, int responseTimeoutInSeconds) {
    super(ldapUserConfig, responseTimeoutInSeconds);
    this.ldapSearch = ldapSearch;
    this.identifier = identifier;
  }
}
