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
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.LogzConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.LogzConfigYamlHandler;

import java.io.IOException;
import java.util.Arrays;

public class LogzConfigYamlHandlerTest extends BaseVerificationProviderYamlHandlerTest {
  @InjectMocks @Inject private LogzConfigYamlHandler yamlHandler;

  public static final String logzUrl = "https://wingsnfr.saas.appdynamics.com:443/controller";

  private String invalidYamlContent = "logzUrl_controller: http://localhost\n"
      + "token : safeharness:kdT-tC2dTNCyY2pJJzSN9A\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: Logz";

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String logzProviderName = "Logz" + System.currentTimeMillis();

    // 1. Create Logz verification record
    SettingAttribute settingAttributeSaved = createJenkinsVerificationProvider(logzProviderName);
    assertEquals(logzProviderName, settingAttributeSaved.getName());
    String yamlFilePath = getYamlFilePath(logzProviderName);
    SettingAttribute fetchedSettingAttribute = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    verify(settingAttributeSaved, fetchedSettingAttribute);

    // 2. update logzUrl and get Yalm String from SettingAttribute
    ((LogzConfig) settingAttributeSaved.getValue()).setLogzUrl(logzUrl + "_1");
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);

    // 3. call upsert() using this yamlContent
    ChangeContext<Yaml> changeContext = getChangeContext(yamlContent, yamlFilePath, Yaml.class, yamlHandler);
    SettingAttribute settingAttributeFromYaml = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    verify(settingAttributeSaved, settingAttributeFromYaml);

    // 4. delete Logz verification provider record using yaml
    yamlHandler.delete(changeContext);
    SettingAttribute settingAttributeDeleted = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNull(settingAttributeDeleted);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String logzProviderName = "Logz" + System.currentTimeMillis();
    // 1. Create Logz verification provider record
    SettingAttribute settingAttributeSaved = createJenkinsVerificationProvider(logzProviderName);
    String yamlFilePath = getYamlFilePath(logzProviderName);

    // 2. Make sure, yaml filepath fetches correct SettingAttribute
    SettingAttribute settingAttribute = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNotNull(settingAttribute);

    // 3. Use invalid yaml path to make sure it fails
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);
    ChangeContext<Yaml> changeContext = getChangeContext(yamlContent, invalidYamlPath, Yaml.class, yamlHandler);
    try {
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (WingsException ex) {
    }

    // 4. Use invalid yaml content (missing encrypted password) to make sure it fails
    try {
      changeContext = getChangeContext(invalidYamlContent, yamlFilePath, Yaml.class, yamlHandler);
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (UnrecognizedPropertyException ex) {
    }
  }

  private SettingAttribute createJenkinsVerificationProvider(String logzProviderName) {
    // Generate Logz verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    LogzConfig logzConfig = new LogzConfig();
    logzConfig.setAccountId(ACCOUNT_ID);
    logzConfig.setLogzUrl(logzUrl);
    logzConfig.setToken(token.toCharArray());

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(logzProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(logzConfig)
                                    .build());
  }
}
