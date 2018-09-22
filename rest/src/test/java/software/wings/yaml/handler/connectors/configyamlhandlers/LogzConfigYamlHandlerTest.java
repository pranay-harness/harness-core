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
import software.wings.beans.config.LogzConfig;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.LogzConfigYamlHandler;

import java.io.IOException;

public class LogzConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private LogzConfigYamlHandler yamlHandler;

  public static final String url = "https://wingsnfr.saas.appdynamics.com:443/controller";

  private String invalidYamlContent = "url_controller: http://localhost\n"
      + "token : safeharness:kdT-tC2dTNCyY2pJJzSN9A\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: Logz";

  private Class yamlClass = LogzConfig.Yaml.class;
  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String logzProviderName = "Logz" + System.currentTimeMillis();

    // 1. Create Logz verification record
    SettingAttribute settingAttributeSaved = createJenkinsVerificationProvider(logzProviderName);
    assertEquals(logzProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(logzProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String logzProviderName = "Logz" + System.currentTimeMillis();

    // 1. Create Logz verification provider record
    SettingAttribute settingAttributeSaved = createJenkinsVerificationProvider(logzProviderName);
    testFailureScenario(generateSettingValueYamlConfig(logzProviderName, settingAttributeSaved));
  }

  private SettingAttribute createJenkinsVerificationProvider(String logzProviderName) {
    // Generate Logz verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    LogzConfig logzConfig = new LogzConfig();
    logzConfig.setAccountId(ACCOUNT_ID);
    logzConfig.setLogzUrl(url);
    logzConfig.setToken(token.toCharArray());

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(logzProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(logzConfig)
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
        .configclazz(LogzConfig.class)
        .updateMethodName("setLogzUrl")
        .currentFieldValue(url)
        .build();
  }
}
