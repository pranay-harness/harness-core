package software.wings.yaml.handler.connectors.configyamlhandlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.AzureConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.AzureConfigYamlHandler;

import java.io.IOException;

public class AzureConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
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
  public void testCRUDAndGet() throws HarnessException, IOException {
    String azureConfigName = "Azure" + System.currentTimeMillis();

    SettingAttribute settingAttributeSaved = createAzureConfigProvider(azureConfigName);
    assertEquals(azureConfigName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(azureConfigName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String azureConfigName = "Azure" + System.currentTimeMillis();

    SettingAttribute settingAttributeSaved = createAzureConfigProvider(azureConfigName);
    testFailureScenario(generateSettingValueYamlConfig(azureConfigName, settingAttributeSaved));
  }

  private SettingAttribute createAzureConfigProvider(String azureConfigName) {
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CLOUD_PROVIDER)
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
