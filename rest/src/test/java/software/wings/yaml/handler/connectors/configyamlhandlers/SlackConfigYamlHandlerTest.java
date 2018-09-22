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
import software.wings.beans.SlackConfig;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.collaborationprovider.SlackConfigYamlHandler;

import java.io.IOException;

public class SlackConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private SlackConfigYamlHandler yamlHandler;

  public static final String url = "https://url.com";

  private String invalidYamlContent = "outgoingWebhookUrl_invalid: url.com\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: SLACK";

  private Class yamlClass = SlackConfig.Yaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String name = "Slack" + System.currentTimeMillis();

    // 1. Create Slack verification record
    SettingAttribute settingAttributeSaved = createSlackVerificationProvider(name);
    assertEquals(name, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(name, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String name = "Slack" + System.currentTimeMillis();

    // 1. Create Slack verification provider record
    SettingAttribute settingAttributeSaved = createSlackVerificationProvider(name);
    testFailureScenario(generateSettingValueYamlConfig(name, settingAttributeSaved));
  }

  private SettingAttribute createSlackVerificationProvider(String name) {
    // Generate Slack verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(name)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(SlackConfig.Builder.aSlackConfig().withOutgoingWebhookUrl(url).build())
                                    .build());
  }

  private SettingValueYamlConfig generateSettingValueYamlConfig(String name, SettingAttribute settingAttributeSaved) {
    return SettingValueYamlConfig.builder()
        .yamlHandler(yamlHandler)
        .yamlClass(yamlClass)
        .settingAttributeSaved(settingAttributeSaved)
        .yamlDirPath(collaborationProviderYamlDir)
        .invalidYamlContent(invalidYamlContent)
        .name(name)
        .configclazz(SlackConfig.class)
        .updateMethodName("setOutgoingWebhookUrl")
        .currentFieldValue(url)
        .build();
  }
}
