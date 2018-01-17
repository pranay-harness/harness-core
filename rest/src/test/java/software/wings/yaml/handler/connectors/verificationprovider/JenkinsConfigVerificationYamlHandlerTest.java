package software.wings.yaml.handler.connectors.verificationprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsConfig.VerificationYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.JenkinsConfigVerificationYamlHandler;

import java.io.IOException;
import java.util.Arrays;

public class JenkinsConfigVerificationYamlHandlerTest extends BaseVerificationProviderYamlHandlerTest {
  @InjectMocks @Inject private JenkinsConfigVerificationYamlHandler yamlHandler;
  public static final String jenkinsUrl = "https://jenkins.wings.software";

  private String invalidYamlContent = "url_jenkins: https://jenkins.wings.software\n"
      + "username: username\n"
      + "password: safeharness:#\n"
      + "type: JENKINS";

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String jenkinsProviderName = "Jenkins" + System.currentTimeMillis();

    // 1. Create Jenkins verification record
    SettingAttribute settingAttributeSaved = createJenkinsVerificationProvider(jenkinsProviderName);
    assertEquals(jenkinsProviderName, settingAttributeSaved.getName());

    String yamlFilePath = getYamlFilePath(jenkinsProviderName);
    SettingAttribute fetchedSettingAttribute = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNotNull(fetchedSettingAttribute);
    verify(settingAttributeSaved, fetchedSettingAttribute);

    // 2. update jenkinsUrl and get Yalm String from SettingAttribute
    ((JenkinsConfig) settingAttributeSaved.getValue()).setUsername(jenkinsUrl + "_1");
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);

    // 3. call upsert() using this yamlContent
    ChangeContext<VerificationYaml> changeContext =
        getChangeContext(yamlContent, yamlFilePath, VerificationYaml.class, yamlHandler);
    SettingAttribute settingAttributeFromYaml = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    verify(settingAttributeSaved, settingAttributeFromYaml);

    // 4. delete jenkins verification provider record using yaml
    yamlHandler.delete(changeContext);
    SettingAttribute settingAttributeDeleted = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNull(settingAttributeDeleted);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String jenkinsProviderName = "Jenkins" + System.currentTimeMillis();
    // 1. Create jenkins verification provider record
    SettingAttribute settingAttributeSaved = createJenkinsVerificationProvider(jenkinsProviderName);
    String yamlFilePath = getYamlFilePath(jenkinsProviderName);

    // 2. Make sure, yaml filepath fetches correct SettingAttribute
    SettingAttribute fetchedSettingAttribute = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNotNull(fetchedSettingAttribute);

    // 3. Use invalid yaml path to make sure it fails
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);
    ChangeContext<VerificationYaml> changeContext =
        getChangeContext(yamlContent, invalidYamlPath, VerificationYaml.class, yamlHandler);
    try {
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (WingsException ex) {
    }

    // 4. Use invalid yaml content (missing encrypted password) to make sure it fails
    try {
      changeContext = getChangeContext(invalidYamlContent, yamlFilePath, VerificationYaml.class, yamlHandler);
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (UnrecognizedPropertyException ex) {
    }
  }

  private SettingAttribute createJenkinsVerificationProvider(String jenkinsProviderName) {
    // Generate Jenkins verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(jenkinsProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(JenkinsConfig.builder()
                                                   .jenkinsUrl(jenkinsUrl)
                                                   .username(userName)
                                                   .accountId(ACCOUNT_ID)
                                                   .password(password.toCharArray())
                                                   .build())
                                    .build());
  }
}
