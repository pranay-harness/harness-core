package io.harness.testframework.restutils;

import static org.junit.Assert.assertNotNull;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.restassured.path.json.JsonPath;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;

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
    assertNotNull(setAttrResponse);
    return setAttrResponse.getString("resource.uuid").trim();
  }

  public static void deleteSSHKey(String bearerToken, String settingAttrId, String accountId) {
    SettingsUtils.delete(bearerToken, accountId, settingAttrId);
  }
}