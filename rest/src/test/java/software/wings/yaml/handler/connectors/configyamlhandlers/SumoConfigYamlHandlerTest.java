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
import software.wings.beans.SumoConfig;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.SumoConfigYamlHandler;

import java.io.IOException;

public class SumoConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private SumoConfigYamlHandler yamlHandler;

  public static final String url = "https://api.us2.sumologic.com/api/v1/";

  private String invalidYamlContent = "url_controlller: https://api.us2.sumologic.com/api/v1/\n"
      + "accessId: safeharness:pgWCawZUSnuZYi0lvgfQQQ\n"
      + "accessKey: safeharness:kdT-tC2dTNCyY2pJJzSN9A\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: SUMO\n";

  private Class yamlClass = SumoConfig.Yaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String sumoLogicProviderName = "SumoLogic" + System.currentTimeMillis();

    // 1. Create sumoLogic verification record
    SettingAttribute settingAttributeSaved = createSumoVerificationProvider(sumoLogicProviderName);
    assertEquals(sumoLogicProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(sumoLogicProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String sumoLogicProviderName = "SumoLogic" + System.currentTimeMillis();

    // 1. Create sumoLogic verification provider record
    SettingAttribute settingAttributeSaved = createSumoVerificationProvider(sumoLogicProviderName);
    testFailureScenario(generateSettingValueYamlConfig(sumoLogicProviderName, settingAttributeSaved));
  }

  private SettingAttribute createSumoVerificationProvider(String sumoProviderName) {
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    SumoConfig sumoConfig = new SumoConfig();
    sumoConfig.setAccessId(acessId.toCharArray());
    sumoConfig.setAccessKey(accesskey.toCharArray());
    sumoConfig.setSumoUrl(url);
    sumoConfig.setAccountId(ACCOUNT_ID);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(sumoProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(sumoConfig)
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
        .configclazz(SumoConfig.class)
        .updateMethodName("setSumoUrl")
        .currentFieldValue(url)
        .build();
  }
}
