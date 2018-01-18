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
import software.wings.beans.BambooConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.artifactserver.BambooConfigYamlHandler;

import java.io.IOException;

public class BambooConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private BambooConfigYamlHandler yamlHandler;

  public static final String url = "http://ec2-34-205-16-35.compute-1.amazonaws.com:8085/";

  private String invalidYamlContent = "url_invalid: http://ec2-34-205-16-35.compute-1.amazonaws.com:8085/\n"
      + "username: username\n"
      + "password: safeharness:2VX9g3DkTFa63TuO6rI8rQ\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: BAMBOO";

  private Class yamlClass = BambooConfig.Yaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String bambooProviderName = "Bamboo" + System.currentTimeMillis();

    // 1. Create Bamboo verification record
    SettingAttribute settingAttributeSaved = createBambooVerificationProvider(bambooProviderName);
    assertEquals(bambooProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(bambooProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String bambooProviderName = "Bamboo" + System.currentTimeMillis();

    // 1. Create Bamboo verification provider record
    SettingAttribute settingAttributeSaved = createBambooVerificationProvider(bambooProviderName);
    testFailureScenario(generateSettingValueYamlConfig(bambooProviderName, settingAttributeSaved));
  }

  private SettingAttribute createBambooVerificationProvider(String bambooProviderName) {
    // Generate Bamboo verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(bambooProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(BambooConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .bambooUrl(url)
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
        .yamlDirPath(artifactServerYamlDir)
        .invalidYamlContent(invalidYamlContent)
        .name(name)
        .configclazz(BambooConfig.class)
        .updateMethodName("setBambooUrl")
        .currentFieldValue(url)
        .build();
  }
}
