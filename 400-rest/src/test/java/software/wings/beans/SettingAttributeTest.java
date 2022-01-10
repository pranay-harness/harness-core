/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.rule.Owner;

import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.service.impl.jenkins.JenkinsUtils;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SettingAttributeTest extends CategoryTest {
  private static final String PASSWORD = "password";
  private static final String TOKEN = "token";
  private static final Random random = new Random();

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForNullValue() {
    List<String> secretIds = prepareSettingAttribute(null).fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).isEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForGCSHelm() {
    List<String> secretIds = prepareSettingAttribute(GCSHelmRepoConfig.builder().build()).fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).isEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForDocker() {
    List<String> secretIds = prepareSettingAttribute(
        DockerConfig.builder().dockerRegistryUrl("docker.registry").encryptedPassword(PASSWORD).build())
                                 .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).containsExactly(PASSWORD);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnEmptyWhenAwsCPWithDelegateOption() {
    AwsConfig awsConfig = AwsConfig.builder().useEc2IamCredentials(true).build();
    List<String> secretIds = awsConfig.fetchRelevantEncryptedSecrets();

    assertThat(secretIds).isEmpty();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnSecretKeyWhenAwsCPWithNonDelegateOption() {
    AwsConfig awsConfig = AwsConfig.builder().useEc2IamCredentials(false).encryptedSecretKey(PASSWORD).build();
    List<String> secretIds = awsConfig.fetchRelevantEncryptedSecrets();

    assertThat(secretIds).containsExactly(PASSWORD);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnAllEncryptedSecretsForNullAuthType() {
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().build();
    List<String> secretIds = kubernetesClusterConfig.fetchRelevantEncryptedSecrets();
    assertThat(secretIds).hasSize(9);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnAllEncryptedSecretsForNoneAuthType() {
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().authType(KubernetesClusterAuthType.NONE).build();
    List<String> secretIds = kubernetesClusterConfig.fetchRelevantEncryptedSecrets();
    assertThat(secretIds).hasSize(9);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnRelevantEncryptedSecretsForOIDCAuthType() {
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().authType(KubernetesClusterAuthType.OIDC).build();
    List<String> secretIds = kubernetesClusterConfig.fetchRelevantEncryptedSecrets();
    assertThat(secretIds).hasSize(3);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnRelevantEncryptedSecretsForServiceAccountAuthType() {
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().authType(KubernetesClusterAuthType.SERVICE_ACCOUNT).build();
    List<String> secretIds = kubernetesClusterConfig.fetchRelevantEncryptedSecrets();
    assertThat(secretIds).hasSize(2);
  }
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnRelevantEncryptedSecretsForClientKeyAuthType() {
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().authType(KubernetesClusterAuthType.CLIENT_KEY_CERT).build();
    List<String> secretIds = kubernetesClusterConfig.fetchRelevantEncryptedSecrets();
    assertThat(secretIds).hasSize(4);
  }
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnRelevantEncryptedSecretsForUserPassAuthType() {
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().authType(KubernetesClusterAuthType.USER_PASSWORD).build();
    List<String> secretIds = kubernetesClusterConfig.fetchRelevantEncryptedSecrets();
    assertThat(secretIds).hasSize(1);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnEmptyWhenInClusterDelegateForK8sCP() {
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().useKubernetesDelegate(true).build();
    List<String> secretIds = kubernetesClusterConfig.fetchRelevantEncryptedSecrets();

    assertThat(secretIds).isEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForJenkins() {
    List<String> secretIds = prepareSettingAttribute(JenkinsConfig.builder()
                                                         .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
                                                         .encryptedPassword(PASSWORD)
                                                         .encryptedToken(TOKEN)
                                                         .build())
                                 .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).containsExactly(PASSWORD);

    secretIds = prepareSettingAttribute(JenkinsConfig.builder()
                                            .authMechanism(JenkinsUtils.TOKEN_FIELD)
                                            .encryptedPassword(PASSWORD)
                                            .encryptedToken(TOKEN)
                                            .build())
                    .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).containsExactly(TOKEN);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFetchRelevantSecretIdsForGit() {
    List<String> secretIds =
        prepareSettingAttribute(GitConfig.builder().keyAuth(false).encryptedPassword(PASSWORD).build())
            .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).containsExactly(PASSWORD);

    secretIds = prepareSettingAttribute(GitConfig.builder().keyAuth(true).encryptedPassword(PASSWORD).build())
                    .fetchRelevantSecretIds();
    assertThat(secretIds).isNotNull();
    assertThat(secretIds).isEmpty();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptedDataMigrationIteration() throws IllegalAccessException {
    long nextSecretMigrationIteration = random.nextLong();
    SettingAttribute settingAttribute =
        prepareSettingAttribute(GitConfig.builder().keyAuth(false).encryptedPassword(PASSWORD).build());
    FieldUtils.writeField(
        settingAttribute, SettingAttributeKeys.nextSecretMigrationIteration, nextSecretMigrationIteration, true);
    assertThat(settingAttribute.obtainNextIteration(SettingAttributeKeys.nextSecretMigrationIteration))
        .isEqualTo(nextSecretMigrationIteration);

    nextSecretMigrationIteration = random.nextLong();
    settingAttribute.updateNextIteration(
        SettingAttributeKeys.nextSecretMigrationIteration, nextSecretMigrationIteration);
    assertThat(settingAttribute.obtainNextIteration(SettingAttributeKeys.nextSecretMigrationIteration))
        .isEqualTo(nextSecretMigrationIteration);

    try {
      settingAttribute.updateNextIteration(generateUuid(), random.nextLong());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      settingAttribute.obtainNextIteration(generateUuid());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  private SettingAttribute prepareSettingAttribute(SettingValue value) {
    return aSettingAttribute().withAccountId(ACCOUNT_ID).withUuid(SETTING_ID).withValue(value).build();
  }
}
