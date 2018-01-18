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
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.SplunkConfig;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.SplunkConfigYamlHandler;

import java.io.IOException;

/**/

public class SplunkConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private SplunkConfigYamlHandler yamlHandler;

  public static final String url = "https://ec2-52-54-103-49.compute-1.amazonaws.com:8089";

  private String invalidYamlContent = "splunk_controllerUrl: https://ec2-52-54-103-49.compute-1.amazonaws.com:8089\n"
      + "username: username\n"
      + "password: amazonkms:#\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: SPLUNK";

  private Class yamlClass = SplunkConfig.Yaml.class;
  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String splunkProviderName = "Splunk" + System.currentTimeMillis();

    // 1. Create newRelic verification record
    SettingAttribute settingAttributeSaved = createSplunkVerificationProvider(splunkProviderName);
    assertEquals(splunkProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(splunkProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String splunkProviderName = "Splunk" + System.currentTimeMillis();

    // 1. Create newRelic verification provider record
    SettingAttribute settingAttributeSaved = createSplunkVerificationProvider(splunkProviderName);
    testFailureScenario(generateSettingValueYamlConfig(splunkProviderName, settingAttributeSaved));
  }

  private SettingAttribute createSplunkVerificationProvider(String splunkProviderName) {
    // Generate Splunk verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(splunkProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(SplunkConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .splunkUrl(url)
                                                   .username(userName)
                                                   .password(password.toCharArray())
                                                   .build())
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
        .configclazz(SplunkConfig.class)
        .updateMethodName("setSplunkUrl")
        .currentFieldValue(url)
        .build();
  }
}
