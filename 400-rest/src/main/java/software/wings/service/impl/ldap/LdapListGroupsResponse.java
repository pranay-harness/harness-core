/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.ldap;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.helpers.ext.ldap.LdapGroupConfig;
import software.wings.helpers.ext.ldap.LdapResponse;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.ldaptive.SearchResult;

@OwnedBy(PL)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapListGroupsResponse {
  SearchResult searchResult;
  LdapResponse ldapResponse;
  LdapGroupConfig ldapGroupConfig;

  public LdapListGroupsResponse(SearchResult searchResult, LdapResponse ldapResponse, LdapGroupConfig ldapGroupConfig) {
    this.searchResult = searchResult;
    this.ldapResponse = ldapResponse;
    this.ldapGroupConfig = ldapGroupConfig;
  }
}
