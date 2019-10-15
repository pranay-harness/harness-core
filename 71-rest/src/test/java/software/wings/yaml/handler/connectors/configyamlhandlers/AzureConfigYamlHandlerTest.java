package software.wings.yaml.handler.connectors.configyamlhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.CCMSettingService;
import io.harness.exception.HarnessException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.AzureConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.AzureConfigYamlHandler;
import software.wings.service.intfc.AccountService;

import java.io.IOException;

public class AzureConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @Mock AccountService accountService;
  @Mock CCMSettingService ccmSettingService;
  @InjectMocks @Inject private AzureConfigYamlHandler yamlHandler;
  public static final String clientId = "dummyClientId";
  public static final String tenantId = "dummyTenantId";
  public static final String key = "dummyKey";

  private String invalidYamlContent = "invalidClientId: dummyClientId\n"
      + "key: amazonkms:zsj_HWfkSF-3li3W-9acHA\n"
      + "tenantId: dummyTenantId\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: AZURE";

  private Class yamlClass = AzureConfig.Yaml.class;

  @Test
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    String azureConfigName = "Azure" + System.currentTimeMillis();

    SettingAttribute settingAttributeSaved = createAzureConfigProvider(azureConfigName);
    assertThat(settingAttributeSaved.getName()).isEqualTo(azureConfigName);

    testCRUD(generateSettingValueYamlConfig(azureConfigName, settingAttributeSaved));
  }

  @Test
  @Category(UnitTests.class)
  public void testFailures() throws HarnessException, IOException {
    String azureConfigName = "Azure" + System.currentTimeMillis();

    SettingAttribute settingAttributeSaved = createAzureConfigProvider(azureConfigName);
    testFailureScenario(generateSettingValueYamlConfig(azureConfigName, settingAttributeSaved));
  }

  private SettingAttribute createAzureConfigProvider(String azureConfigName) {
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(SettingCategory.CLOUD_PROVIDER)
                                    .withName(azureConfigName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(AzureConfig.builder()
                                                   .clientId(clientId)
                                                   .tenantId(tenantId)
                                                   .key(key.toCharArray())
                                                   .accountId(ACCOUNT_ID)
                                                   .build())
                                    .build());
  }

  private SettingValueYamlConfig generateSettingValueYamlConfig(String name, SettingAttribute settingAttributeSaved) {
    return SettingValueYamlConfig.builder()
        .yamlHandler(yamlHandler)
        .yamlClass(yamlClass)
        .settingAttributeSaved(settingAttributeSaved)
        .yamlDirPath(cloudProviderYamlDir)
        .invalidYamlContent(invalidYamlContent)
        .name(name)
        .configclazz(AzureConfig.class)
        .updateMethodName("setClientId")
        .currentFieldValue(clientId)
        .build();
  }
}
