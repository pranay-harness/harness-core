/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.RAGHVENDRA;
import static io.harness.rule.OwnerRule.SAINATH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.AwsConfigYamlHandler;
import software.wings.service.impl.yaml.handler.templatelibrary.SettingValueConfigYamlHandlerTestBase;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.WingsTestConstants;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class AwsConfigYamlHandlerTest extends SettingValueConfigYamlHandlerTestBase {
  @InjectMocks @Inject private AwsConfigYamlHandler yamlHandler;
  @Mock SecretManager secretManager;

  public static final String SAMPLE_STRING = "sample-string";

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testToBeanBothAccessKeyAndAccessKeySecretId() {
    AwsConfig.Yaml yaml = AwsConfig.Yaml.builder().accessKey(SAMPLE_STRING).accessKeySecretId(SAMPLE_STRING).build();

    Change change = Change.Builder.aFileChange()
                        .withAccountId("ABC")
                        .withFilePath("Setup/Cloud Providers/test-harness.yaml")
                        .build();
    ChangeContext<AwsConfig.Yaml> changeContext = ChangeContext.Builder.aChangeContext()
                                                      .withYamlType(YamlType.CLOUD_PROVIDER)
                                                      .withYaml(yaml)
                                                      .withChange(change)
                                                      .build();

    assertThatThrownBy(() -> yamlHandler.toBean(null, changeContext, null))
        .hasMessageContaining("Cannot set both value and secret reference for accessKey field");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testToYamlAssumeIamRoleOnDelegate() {
    AwsConfig awsConfig = AwsConfig.builder().useEc2IamCredentials(true).accessKey(null).secretKey(null).build();
    SettingAttribute settingAttribute = new SettingAttribute();

    settingAttribute.setValue(awsConfig);
    AwsConfig.Yaml yaml = yamlHandler.toYaml(settingAttribute, WingsTestConstants.APP_ID);
    assertThat(yaml.getAccessKey()).isEqualTo(null);
    assertThat(yaml.getSecretKey()).isEqualTo(null);
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testToYamlUseIRSA() {
    AwsConfig awsConfig = AwsConfig.builder().useIRSA(true).accessKey(null).secretKey(null).build();
    SettingAttribute settingAttribute = new SettingAttribute();

    settingAttribute.setValue(awsConfig);
    AwsConfig.Yaml yaml = yamlHandler.toYaml(settingAttribute, WingsTestConstants.APP_ID);
    assertThat(yaml.getAccessKey()).isEqualTo(null);
    assertThat(yaml.getSecretKey()).isEqualTo(null);
    assertThat(yaml.isUseEc2IamCredentials()).isFalse();
    assertThat(yaml.isAssumeCrossAccountRole()).isFalse();
    assertThat(yaml.isUseIRSA()).isTrue();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testToYamlUseEncryptedAccessKey() throws IllegalAccessException {
    String accessKey = "accessKeyValue";
    String secretKey = "secretKeyValue";
    AwsConfig awsConfig = AwsConfig.builder()
                              .useEc2IamCredentials(false)
                              .useEncryptedAccessKey(true)
                              .encryptedSecretKey(secretKey)
                              .encryptedAccessKey(accessKey)
                              .accountId("accountId")
                              .build();
    SettingAttribute settingAttribute = new SettingAttribute();

    settingAttribute.setValue(awsConfig);
    when(secretManager.getEncryptedYamlRef(awsConfig.getAccountId(), awsConfig.getEncryptedAccessKey()))
        .thenReturn(String.valueOf(accessKey));
    when(secretManager.getEncryptedYamlRef(awsConfig.getAccountId(), awsConfig.getEncryptedSecretKey()))
        .thenReturn(secretKey);
    AwsConfig.Yaml yaml = yamlHandler.toYaml(settingAttribute, WingsTestConstants.APP_ID);
    assertThat(yaml.getAccessKeySecretId()).isEqualTo(String.valueOf(accessKey));
    assertThat(yaml.getAccessKey()).isEqualTo(null);
    assertThat(yaml.getSecretKey()).isEqualTo(secretKey);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testToYamlDontUseEncryptedAccessKey() throws IllegalAccessException {
    char[] accessKey = "accessKeyValue".toCharArray();
    String secretKey = "secretKeyValue";
    AwsConfig awsConfig = AwsConfig.builder()
                              .useEc2IamCredentials(false)
                              .useEncryptedAccessKey(false)
                              .accessKey(accessKey)
                              .encryptedSecretKey(secretKey)
                              .accountId("accountId")
                              .build();
    SettingAttribute settingAttribute = new SettingAttribute();

    settingAttribute.setValue(awsConfig);
    when(secretManager.getEncryptedYamlRef(awsConfig.getAccountId(), awsConfig.getEncryptedSecretKey()))
        .thenReturn(secretKey);
    AwsConfig.Yaml yaml = yamlHandler.toYaml(settingAttribute, WingsTestConstants.APP_ID);
    assertThat(yaml.getAccessKey()).isEqualTo(String.valueOf(accessKey));
    assertThat(yaml.getAccessKeySecretId()).isEqualTo(null);
    assertThat(yaml.getSecretKey()).isEqualTo(secretKey);
  }

  @Test
  @Owner(developers = SAINATH)
  @Category(UnitTests.class)
  public void testToYamlRegion() {
    String defaultRegion = "defaultRegion";
    String accountId = "accountId";
    String secretKey = "secretKey";

    AwsConfig awsConfig =
        AwsConfig.builder().encryptedSecretKey(secretKey).accountId(accountId).defaultRegion(defaultRegion).build();
    SettingAttribute settingAttribute = new SettingAttribute();

    settingAttribute.setValue(awsConfig);
    when(secretManager.getEncryptedYamlRef(awsConfig.getAccountId(), awsConfig.getEncryptedSecretKey()))
        .thenReturn(secretKey);
    AwsConfig.Yaml yaml = yamlHandler.toYaml(settingAttribute, WingsTestConstants.APP_ID);

    assertThat(yaml.getDefaultRegion()).isEqualTo(defaultRegion);
  }

  @Test
  @Owner(developers = SAINATH)
  @Category(UnitTests.class)
  public void testToBeanRegion() {
    String defaultRegion = "defaultRegion";
    AwsConfig.Yaml yaml = AwsConfig.Yaml.builder().defaultRegion(defaultRegion).build();

    Change change = Change.Builder.aFileChange()
                        .withAccountId("accountId")
                        .withFilePath("Setup/Cloud Providers/test-harness.yaml")
                        .build();
    ChangeContext<AwsConfig.Yaml> changeContext = ChangeContext.Builder.aChangeContext()
                                                      .withYamlType(YamlType.CLOUD_PROVIDER)
                                                      .withYaml(yaml)
                                                      .withChange(change)
                                                      .build();

    SettingAttribute settingAttribute = yamlHandler.toBean(null, changeContext, null);
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    assertThat(awsConfig.getDefaultRegion()).isEqualTo(defaultRegion);
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testToBeanUseIRSA() {
    AwsConfig.Yaml yaml = AwsConfig.Yaml.builder().useIRSA(true).build();

    Change change = Change.Builder.aFileChange()
                        .withAccountId("accountId")
                        .withFilePath("Setup/Cloud Providers/test-harness.yaml")
                        .build();
    ChangeContext<AwsConfig.Yaml> changeContext = ChangeContext.Builder.aChangeContext()
                                                      .withYamlType(YamlType.CLOUD_PROVIDER)
                                                      .withYaml(yaml)
                                                      .withChange(change)
                                                      .build();

    SettingAttribute settingAttribute = yamlHandler.toBean(null, changeContext, null);
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    assertThat(awsConfig.isUseIRSA()).isTrue();
    assertThat(awsConfig.isAssumeCrossAccountRole()).isFalse();
    assertThat(awsConfig.isUseEc2IamCredentials()).isFalse();
  }
}
