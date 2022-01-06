/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.SSO.LDAP;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class LdapIntegrationTestConstants {
  static String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  static String USER_GROUP_ID = "rqIrxO78S0SQMHn4Tt9rPw";

  static final String ACCOUNT_ID_PARAM = "accountId";
  static final String INVALID_TOKEN = "INVALID_TOKEN";
  static final String ADMIN_HARNESS_ID = "admin@harness.io";
  static final String LDAP_GROUP_DN_TO_LINK_TO_HARNESS_GROUP = "CN=Administrators,CN=Builtin,DC=harness,DC=io";

  // Authentication Request
  static final String email = "aman@harness.io";
  static final String pass = "Pleasepls1";
  static final String adminAccountLdapPass = "Pleasepls2";

  // Connection Settings
  static final String bindHost = "ip-172-31-37-58.ec2.internal";
  static final String bindDn = "CN=Aman,CN=Users,DC=harness,DC=io";
  static final String bindDnPassword = "Pleasepls1";
  static final int connectionPort = 389;
  static final boolean sslEabled = false;
  static final boolean referralsEnabled = true;
  static final int maxReferralHops = 5;
  static final int connectionTimeout = 50000;
  static final int responseTimeout = 50000;

  // User Settings
  static final String userSettingBaseDn1 = "CN=Users,DC=harness,DC=io";
  static final String userSettingSearchFilter = "(objectClass=organizationalPerson)";
  static final String userSettingEmailAttr = "userPrincipalName";
  static final String userSettingCn = "cn";
  static final String groupMembershipAttr = "memberOf";
  static final String referencedUserAttr = "dn";

  // Group Settings
  public static final String LDAP_GROUP_NAME = "Administrators";
  static final String groupSettingBaseDn1 = "CN=Administrators,CN=Builtin,DC=harness,DC=io";
  static final String groupSettingBaseDn2 = "CN=HelpLibraryUpdaters,CN=Users,DC=harness,DC=io";
  static final String groupSettingSearchFilter = "(objectClass=group)";
  static final String groupSettingCn = "cn";
  static final String groupSettingDescriptionAttr = "description";
  static final String userMembershipAttr = "member";
}
