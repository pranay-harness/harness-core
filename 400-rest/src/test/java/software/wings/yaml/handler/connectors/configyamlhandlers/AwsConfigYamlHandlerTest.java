package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.BOJANA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.AwsConfigYamlHandler;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.WingsTestConstants;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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
}
