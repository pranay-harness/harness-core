/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.ldap;

import software.wings.beans.sso.LdapSearchConfig;

public interface LdapUserConfig extends LdapSearchConfig {
  String getSearchFilter();

  String getEmailAttr();

  String getDisplayNameAttr();

  String getGroupMembershipAttr();

  String getUserFilter();

  String getLoadUsersFilter();

  String getGroupMembershipFilter(String groupDn);

  String getFallbackGroupMembershipFilter(String groupName);
}
