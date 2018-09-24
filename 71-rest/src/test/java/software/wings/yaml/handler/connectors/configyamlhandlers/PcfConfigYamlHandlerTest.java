package software.wings.yaml.handler.connectors.configyamlhandlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.PcfConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.PcfConfigYamlHandler;

import java.io.IOException;

public class PcfConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private PcfConfigYamlHandler yamlHandler;
  public static final String endpointUrl = "link.com";

  private String invalidYamlContent = "invalidClientId: dummyClientId\n"
      + "key: amazonkms:zsj_HWfkSF-3li3W-9acHA\n"
      + "tenantId: dummyTenantId\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: AZURE";

  private Class yamlClass = PcfConfig.Yaml.class;

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String pcfConfigName = "Pcf" + System.currentTimeMillis();

    SettingAttribute settingAttributeSaved = createPCFConfigProvider(pcfConfigName);
    assertEquals(pcfConfigName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(pcfConfigName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String pcfConfigName = "Pcf" + System.currentTimeMillis();

    SettingAttribute settingAttributeSaved = createPCFConfigProvider(pcfConfigName);
    testFailureScenario(generateSettingValueYamlConfig(pcfConfigName, settingAttributeSaved));
  }

  private SettingAttribute createPCFConfigProvider(String pcfConfigName) {
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CLOUD_PROVIDER)
                                    .withName(pcfConfigName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(PcfConfig.builder()
                                                   .username(USER_NAME)
                                                   .endpointUrl(endpointUrl)
                                                   .password(PASSWORD)
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
        .configclazz(PcfConfig.class)
        .updateMethodName("setUsername")
        .currentFieldValue(USER_NAME)
        .build();
  }
}
