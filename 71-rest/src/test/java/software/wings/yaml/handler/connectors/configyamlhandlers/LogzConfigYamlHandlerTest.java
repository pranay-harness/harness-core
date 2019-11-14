package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.HarnessException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.config.LogzConfig;
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
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    String logzProviderName = "Logz" + System.currentTimeMillis();

    // 1. Create Logz verification record
    SettingAttribute settingAttributeSaved = createJenkinsVerificationProvider(logzProviderName);
    assertThat(settingAttributeSaved.getName()).isEqualTo(logzProviderName);

    testCRUD(generateSettingValueYamlConfig(logzProviderName, settingAttributeSaved));
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
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
                                    .withCategory(SettingCategory.CONNECTOR)
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
