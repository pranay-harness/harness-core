/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.restutils;

import static io.harness.shell.AccessType.KEY;

import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;

import io.restassured.path.json.JsonPath;

public class SSHKeysUtils {
  public static String createSSHKey(String bearerToken, String sshKeyName, String accountId) {
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(sshKeyName)
            .withAccountId(accountId)
            .withCategory(SettingCategory.SETTING)
            .withValue(aHostConnectionAttributes()
                           .withConnectionType(SSH)
                           .withAccessType(KEY)
                           .withAccountId(accountId)
                           .withUserName("ec2-user")
                           .withSshPort(22)
                           .withKey(new ScmSecret().decryptToCharArray(new SecretName("ec2_qe_ssh_key")))
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    return setAttrResponse.getString("resource.uuid").trim();
  }

  public static void deleteSSHKey(String bearerToken, String settingAttrId, String accountId) {
    SettingsUtils.delete(bearerToken, accountId, settingAttrId);
  }
}
