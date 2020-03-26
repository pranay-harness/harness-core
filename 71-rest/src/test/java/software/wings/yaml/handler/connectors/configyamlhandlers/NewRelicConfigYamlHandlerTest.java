package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.NewRelicConfigYamlHandler;

public class NewRelicConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private NewRelicConfigYamlHandler yamlHandler;

  public static final String url = "https://api.newrelic.com";

  private String invalidYamlContent = "apiKey_newrelic: amazonkms:C7cBDpxHQzG5rv30tvZDgw\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: NEW_RELIC";

  private Class yamlClass = NewRelicConfig.Yaml.class;

  @Before
  public void setUp() throws Exception {}

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws Exception {
    String newRelicProviderName = "newRelic" + System.currentTimeMillis();

    // 1. Create newRelic verification record
    SettingAttribute settingAttributeSaved = createNewRelicProviderNameVerificationProvider(newRelicProviderName);
    assertThat(settingAttributeSaved.getName()).isEqualTo(newRelicProviderName);

    testCRUD(generateSettingValueYamlConfig(newRelicProviderName, settingAttributeSaved));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFailures() throws Exception {
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
            .withCategory(SettingCategory.CONNECTOR)
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
