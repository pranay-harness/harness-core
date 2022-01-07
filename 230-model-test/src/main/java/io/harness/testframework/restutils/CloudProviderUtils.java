/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;

import io.restassured.path.json.JsonPath;

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
