package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.ElkConfigYamlHandler;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class ElkConfigYamlHandlerTest extends SettingValueConfigYamlHandlerTestBase {
  @InjectMocks @Inject private ElkConfigYamlHandler yamlHandler;

  public static final String url = "https://ec2-34-207-78-53.compute-1.amazonaws.com:9200/";

  private String invalidYamlContent = "url_controller: https://ec2-34-207-78-53.compute-1.amazonaws.com:9200/\n"
      + "username: elastic\n"
      + "password: safeharness:_DoDJU9JRTSJJYxv3S6wNQ\n"
      + "connectorType: ELASTIC_SEARCH_SERVER\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: ELK";

  private Class yamlClass = ElkConfig.Yaml.class;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws Exception {
    String elkProviderName = "Elk" + System.currentTimeMillis();

    // 1. Create elk verification record
    SettingAttribute settingAttributeSaved = createElkVerificationProvider(elkProviderName);
    assertThat(settingAttributeSaved.getName()).isEqualTo(elkProviderName);

    testCRUD(generateSettingValueYamlConfig(elkProviderName, settingAttributeSaved));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFailures() throws Exception {
    String elkProviderName = "Elk" + System.currentTimeMillis();

    // 1. Create elk verification provider record
    SettingAttribute settingAttributeSaved = createElkVerificationProvider(elkProviderName);
    testFailureScenario(generateSettingValueYamlConfig(elkProviderName, settingAttributeSaved));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testNullValidationType() {
    String accountId = generateUuid();
    String appId = generateUuid();
    ElkConfig config = ElkConfig.builder().elkUrl(url).accountId(accountId).validationType(null).build();
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);
    ElkConfig.Yaml yaml = yamlHandler.toYaml(attribute, appId);

    assertThat(yaml).isNotNull();
    assertThat(yaml.getElkUrl()).isEqualTo(url);
    assertThat(yaml.getUsername()).isNull();
    assertThat(yaml.getValidationType()).isNull();
    assertThat(yaml.getPassword()).isNull();
  }

  private SettingAttribute createElkVerificationProvider(String elkProviderName) {
    // Generate Elk verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(ACCOUNT_ID);
    elkConfig.setElkConnector(ElkConnector.ELASTIC_SEARCH_SERVER);
    elkConfig.setElkUrl(url);
    elkConfig.setKibanaVersion("0");
    elkConfig.setUsername(userName);
    elkConfig.setPassword(createSecretText(ACCOUNT_ID, "password", password).toCharArray());

    return settingsService.save(aSettingAttribute()
                                    .withCategory(SettingCategory.CONNECTOR)
                                    .withName(elkProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(elkConfig)
                                    .build());
  }

  private SettingValueYamlConfig generateSettingValueYamlConfig(String name, SettingAttribute settingAttributeSaved) {
    return SettingValueYamlConfig.builder()
        .yamlHandler(yamlHandler)
        .yamlClass(yamlClass)
        .settingAttributeSaved(settingAttributeSaved)
        .yamlDirPath(verificationProviderYamlDir)
        .invalidYamlContent(invalidYamlContent)
        .name(name)
        .configclazz(ElkConfig.class)
        .updateMethodName("setElkUrl")
        .currentFieldValue(url)
        .build();
  }
}
