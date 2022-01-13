/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.secrets;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretText;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.SecretsUtils;
import io.harness.testframework.restutils.SecretsRestUtils;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import com.google.inject.Inject;
import io.restassured.mapper.ObjectMapperType;
import java.util.List;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author marklu on 2019-05-08
 */
@Slf4j
public class AwsSecretsManagerServiceFunctionalTest extends AbstractFunctionalTest {
  private static final String ASM_NAME = "AWS Secrets Manager";

  @Inject private SecretManagementDelegateService secretManagementDelegateService;
  @Inject private AwsSecretsManagerService secretsManagerService;

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testCRUDSecretsWithAwsSecretsManager() {
    AwsSecretsManagerConfig secretsManagerConfig =
        AwsSecretsManagerConfig.builder()
            .name(ASM_NAME)
            .accessKey("AKIA5GUB5GGCID6JJB7D")
            .secretKey(new ScmSecret().decryptToString(new SecretName("plat_aws_secrets_manager_secret")))
            .region("us-east-1")
            .secretNamePrefix("foo/bar")
            .build();
    secretsManagerConfig.setAccountId(getAccount().getUuid());
    secretsManagerConfig.setDefault(true);

    String secretsManagerId = null;
    try {
      secretsManagerId = addAwsSecretsManager(secretsManagerConfig);
      assertThat(secretsManagerId).isNotNull();
      log.info("AWS Secrets Manager config created.");

      List<AwsSecretsManagerConfig> secretsManagerConfigs = listConfigs(getAccount().getUuid());
      assertThat(secretsManagerConfigs.size() > 0).isTrue();

      String secretName = "MySecret";
      String secretValue = "MyValue";
      SecretText secretText = SecretsUtils.createSecretTextObject(secretName, secretValue);

      String secretId = SecretsRestUtils.addSecret(getAccount().getUuid(), bearerToken, secretText);
      assertThat(StringUtils.isNotBlank(secretId)).isTrue();

      secretName = "MySecret-Updated";
      secretValue = "MyValue-Updated";
      secretText.setName(secretName);
      secretText.setValue(secretValue);
      boolean isUpdationDone = SecretsRestUtils.updateSecret(getAccount().getUuid(), bearerToken, secretId, secretText);
      assertThat(isUpdationDone).isTrue();

      // Verifying the secret decryption
      List<EncryptedData> encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
      assertThat(encryptedDataList.size() > 0).isTrue();

      boolean isDeletionDone = SecretsRestUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretId);
      assertThat(isDeletionDone).isTrue();
    } finally {
      if (secretsManagerId != null) {
        deleteAwsSecretsManager(getAccount().getUuid(), secretsManagerId);
        log.info("AWS Secrets Manager deleted.");
      }
    }
  }

  private String addAwsSecretsManager(AwsSecretsManagerConfig secretsManagerConfig) {
    RestResponse<String> restResponse = Setup.portal()
                                            .auth()
                                            .oauth2(bearerToken)
                                            .queryParam("accountId", secretsManagerConfig.getAccountId())
                                            .body(secretsManagerConfig, ObjectMapperType.GSON)
                                            .post("/aws-secrets-manager")
                                            .as(new GenericType<RestResponse<String>>() {}.getType());
    return restResponse.getResource();
  }

  private Boolean deleteAwsSecretsManager(String accountId, String secretsManagerConfigId) {
    RestResponse<Boolean> restResponse = Setup.portal()
                                             .auth()
                                             .oauth2(bearerToken)
                                             .queryParam("accountId", accountId)
                                             .queryParam("configId", secretsManagerConfigId)
                                             .delete("/aws-secrets-manager")
                                             .as(new GenericType<RestResponse<Boolean>>() {}.getType());
    return restResponse.getResource();
  }

  private static List<AwsSecretsManagerConfig> listConfigs(String accountId) {
    RestResponse<List<AwsSecretsManagerConfig>> secretsResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/secrets/list-values")
            .as(new GenericType<RestResponse<List<AwsSecretsManagerConfig>>>() {}.getType());
    return secretsResponse.getResource();
  }
}
