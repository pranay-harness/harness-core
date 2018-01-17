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
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SplunkConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.SplunkConfigYamlHandler;

import java.io.IOException;
import java.util.Arrays;

/**/

public class SplunkConfigYamlHandlerTest extends BaseVerificationProviderYamlHandlerTest {
  @InjectMocks @Inject private SplunkConfigYamlHandler yamlHandler;

  public static final String splunkUrl = "https://ec2-52-54-103-49.compute-1.amazonaws.com:8089";

  private String invalidYamlContent = "splunk_controllerUrl: https://ec2-52-54-103-49.compute-1.amazonaws.com:8089\n"
      + "username: username\n"
      + "password: amazonkms:#\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: SPLUNK";

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String splunkProviderName = "Splunk" + System.currentTimeMillis();

    // 1. Create splunk verification record
    SettingAttribute settingAttributeSaved = createSplunkVerificationProvider(splunkProviderName);
    assertEquals(splunkProviderName, settingAttributeSaved.getName());
    String yamlFilePath = getYamlFilePath(splunkProviderName);
    SettingAttribute fetchedSettingAttribute = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNotNull(fetchedSettingAttribute);
    verify(settingAttributeSaved, fetchedSettingAttribute);

    // 2. update splunkUrl and get Yalm String from SettingAttribute
    ((SplunkConfig) settingAttributeSaved.getValue()).setUsername(splunkUrl + "_1");
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);

    // 3. call upsert() using this yamlContent
    ChangeContext<Yaml> changeContext = getChangeContext(yamlContent, yamlFilePath, Yaml.class, yamlHandler);
    SettingAttribute settingAttributeFromYaml = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    verify(settingAttributeSaved, settingAttributeFromYaml);

    // 4. delete Splunk verification provider record using yaml
    yamlHandler.delete(changeContext);
    SettingAttribute settingAttributeDeleted = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNull(settingAttributeDeleted);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String splunkProviderName = "Splunk" + System.currentTimeMillis();

    // 1. Create Splunk verification provider record
    SettingAttribute settingAttributeSaved = createSplunkVerificationProvider(splunkProviderName);
    String validYamlFilePath = getYamlFilePath(splunkProviderName);

    // 2. Make sure, yaml filepath fetches correct SettingAttribute
    SettingAttribute settingAttribute = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNotNull(settingAttribute);

    // 3. Now, Use invalid yaml path and make sure it upsertFromYaml fails
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);
    ChangeContext<Yaml> changeContext = getChangeContext(yamlContent, invalidYamlPath, Yaml.class, yamlHandler);
    try {
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (WingsException ex) {
    }

    // 4. Now, Use invalid yaml content (missing encrypted password) and  make sure upsertFromYaml fails
    try {
      changeContext = getChangeContext(invalidYamlContent, validYamlFilePath, Yaml.class, yamlHandler);
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (UnrecognizedPropertyException ex) {
    }
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
                                                   .splunkUrl(splunkUrl)
                                                   .username(userName)
                                                   .password(password.toCharArray())
                                                   .build())
                                    .build());
  }
}
