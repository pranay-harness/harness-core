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
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.AppDynamicsConfigYamlHandler;

import java.io.IOException;

public class AppDynamicsConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private AppDynamicsConfigYamlHandler yamlHandler;

  public static final String url = "https://wingsnfr.saas.appdynamics.com:443/controller";

  private String invalidYamlContent = "username_appd: username\n"
      + "password: amazonkms:zsj_HWfkSF-3li3W-9acHA\n"
      + "accountName: accountName\n"
      + "controllerUrl: https://wingsnfr.saas.appdynamics.com:443/controller\n"
      + "type: APP_DYNAMICS";

  private Class yamlClass = AppDynamicsConfig.Yaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String appdProviderName = "Appdynamics" + System.currentTimeMillis();

    // 1. Create Appdynamics verification record
    SettingAttribute settingAttributeSaved = createAppdynamicsVerificationProvider(appdProviderName);
    assertEquals(appdProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(appdProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String appdProviderName = "Appdynamics" + System.currentTimeMillis();

    // 1. Create appdynamics verification provider record
    SettingAttribute settingAttributeSaved = createAppdynamicsVerificationProvider(appdProviderName);
    testFailureScenario(generateSettingValueYamlConfig(appdProviderName, settingAttributeSaved));
  }

  private SettingAttribute createAppdynamicsVerificationProvider(String appdProviderName) {
    // Generate appdynamics verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(appdProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(AppDynamicsConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .controllerUrl(url)
                                                   .username(userName)
                                                   .password(password.toCharArray())
                                                   .accountname(accountName)
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
        .configclazz(AppDynamicsConfig.class)
        .updateMethodName("setControllerUrl")
        .currentFieldValue(url)
        .build();
  }
}
