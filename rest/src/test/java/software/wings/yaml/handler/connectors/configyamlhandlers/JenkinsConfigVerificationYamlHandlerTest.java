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
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.JenkinsConfigVerificationYamlHandler;

import java.io.IOException;

public class JenkinsConfigVerificationYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private JenkinsConfigVerificationYamlHandler yamlHandler;
  public static final String url = "https://jenkins.wings.software";

  private String invalidYamlContent = "url_jenkins: https://jenkins.wings.software\n"
      + "username: username\n"
      + "password: safeharness:#\n"
      + "type: JENKINS";

  private Class yamlClass = JenkinsConfig.VerificationYaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String jenkinsProviderName = "Jenkins" + System.currentTimeMillis();

    // 1. Create jenkins verification record
    SettingAttribute settingAttributeSaved = createJenkinsVerificationProvider(jenkinsProviderName);
    assertEquals(jenkinsProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(jenkinsProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String jenkinsProviderName = "Jenkins" + System.currentTimeMillis();

    // 1. Create jenkins verification provider record
    SettingAttribute settingAttributeSaved = createJenkinsVerificationProvider(jenkinsProviderName);
    testFailureScenario(generateSettingValueYamlConfig(jenkinsProviderName, settingAttributeSaved));
  }

  private SettingAttribute createJenkinsVerificationProvider(String jenkinsProviderName) {
    // Generate Jenkins verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(jenkinsProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(JenkinsConfig.builder()
                                                   .jenkinsUrl(url)
                                                   .username(userName)
                                                   .accountId(ACCOUNT_ID)
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
        .configclazz(JenkinsConfig.class)
        .updateMethodName("setJenkinsUrl")
        .currentFieldValue(url)
        .build();
  }
}
