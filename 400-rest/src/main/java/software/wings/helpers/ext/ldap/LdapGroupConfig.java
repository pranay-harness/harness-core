/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.ldap;

import software.wings.beans.sso.LdapSearchConfig;

public interface LdapGroupConfig extends LdapSearchConfig {
  String getSearchFilter();

  String getNameAttr();

  String getDescriptionAttr();

  String getUserMembershipAttr();

  String getReferencedUserAttr();

  String getFilter(String identifier);
}
