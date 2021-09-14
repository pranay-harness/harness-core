/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
