package software.wings.yaml.handler.connectors.configyamlhandlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.NewRelicConfigYamlHandler;

import java.io.IOException;

public class NewRelicConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private NewRelicConfigYamlHandler yamlHandler;

  public static final String url = "https://api.newrelic.com";

  private String invalidYamlContent = "apiKey_newrelic: amazonkms:C7cBDpxHQzG5rv30tvZDgw\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: NEW_RELIC";

  private Class yamlClass = NewRelicConfig.Yaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String newRelicProviderName = "newRelic" + System.currentTimeMillis();

    // 1. Create newRelic verification record
    SettingAttribute settingAttributeSaved = createNewRelicProviderNameVerificationProvider(newRelicProviderName);
    assertEquals(newRelicProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(newRelicProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String newRelicProviderName = "newRelic" + System.currentTimeMillis();

    // 1. Create newRelic verification provider record
    SettingAttribute settingAttributeSaved = createNewRelicProviderNameVerificationProvider(newRelicProviderName);
    testFailureScenario(generateSettingValueYamlConfig(newRelicProviderName, settingAttributeSaved));
  }

  private SettingAttribute createNewRelicProviderNameVerificationProvider(String newRelicProviderName) {
    // Generate newRelic verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName(newRelicProviderName)
            .withAccountId(ACCOUNT_ID)
            .withValue(
                NewRelicConfig.builder().accountId(ACCOUNT_ID).newRelicUrl(url).apiKey(apiKey.toCharArray()).build())
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
        .configclazz(NewRelicConfig.class)
        .updateMethodName(null)
        .currentFieldValue(null)
        .build();
  }
}
