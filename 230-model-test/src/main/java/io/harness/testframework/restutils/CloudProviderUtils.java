package io.harness.testframework.restutils;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.restassured.path.json.JsonPath;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;

public class CloudProviderUtils {
  public static String createAWSCloudProvider(String bearerToken, String cloudPrividerName, String accountId) {
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withName(cloudPrividerName)
            .withAccountId(accountId)
            .withValue(
                AwsConfig.builder()
                    .accessKey(new ScmSecret().decryptToCharArray(new SecretName("qe_aws_playground_access_key")))
                    .secretKey(new ScmSecret().decryptToCharArray(new SecretName("qe_aws_playground_secret_key")))
                    .accountId(accountId)
                    .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    return setAttrResponse.getString("resource.uuid").trim();
  }

  public static String createGCPCloudProvider(String bearerToken, String cloudProviderName, String accountId) {
    JsonPath setAttrResponse = SettingsUtils.createGCP(bearerToken, accountId, cloudProviderName);
    assertThat(setAttrResponse).isNotNull();
    return setAttrResponse.getString("resource.uuid").trim();
  }
}
