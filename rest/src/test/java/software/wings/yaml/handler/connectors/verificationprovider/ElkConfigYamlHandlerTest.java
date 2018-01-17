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
import software.wings.beans.ElkConfig;
import software.wings.beans.ElkConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.ElkConfigYamlHandler;

import java.io.IOException;
import java.util.Arrays;

public class ElkConfigYamlHandlerTest extends BaseVerificationProviderYamlHandlerTest {
  @InjectMocks @Inject private ElkConfigYamlHandler yamlHandler;

  public static final String elkUrl = "https://ec2-34-207-78-53.compute-1.amazonaws.com:9200/";

  private String invalidYamlContent = "elkUrl_controller: https://ec2-34-207-78-53.compute-1.amazonaws.com:9200/\n"
      + "username: elastic\n"
      + "password: safeharness:_DoDJU9JRTSJJYxv3S6wNQ\n"
      + "connectorType: ELASTIC_SEARCH_SERVER\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: ELK";

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String elkProviderName = "Elk" + System.currentTimeMillis();

    // 1. Create Elk verification record
    SettingAttribute settingAttributeSaved = createElkVerificationProvider(elkProviderName);
    assertEquals(elkProviderName, settingAttributeSaved.getName());

    String yamlFilePath = getYamlFilePath(elkProviderName);
    SettingAttribute fetchedSettingAttribute = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNotNull(fetchedSettingAttribute);
    verify(settingAttributeSaved, fetchedSettingAttribute);

    // 2. update elkUrl and get Yalm String from SettingAttribute
    ((ElkConfig) settingAttributeSaved.getValue()).setElkUrl(elkUrl + "_1");
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);

    // 3. call upsert() using this yamlContent
    ChangeContext<Yaml> changeContext = getChangeContext(yamlContent, yamlFilePath, Yaml.class, yamlHandler);
    SettingAttribute settingAttributeFromYaml = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    verify(settingAttributeSaved, settingAttributeFromYaml);

    // 4. delete elk verification provider record using yaml
    yamlHandler.delete(changeContext);
    SettingAttribute settingAttributeDeleted = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNull(settingAttributeDeleted);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String elkProviderName = "Elk" + System.currentTimeMillis();

    // 1. Create elk verification provider record
    SettingAttribute settingAttributeSaved = createElkVerificationProvider(elkProviderName);
    String yamlFilePath = getYamlFilePath(elkProviderName);

    // 2. Make sure, yaml filepath fetches correct SettingAttribute
    SettingAttribute settingAttribute = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
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
      changeContext = getChangeContext(invalidYamlContent, yamlFilePath, Yaml.class, yamlHandler);
      yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
      assertTrue(false);
    } catch (UnrecognizedPropertyException ex) {
    }
  }

  private SettingAttribute createElkVerificationProvider(String elkProviderName) {
    // Generate Elk verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(ACCOUNT_ID);
    elkConfig.setElkConnector(ElkConnector.ELASTIC_SEARCH_SERVER);
    elkConfig.setElkUrl(elkUrl);
    elkConfig.setUsername(userName);
    elkConfig.setPassword(password.toCharArray());

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(elkProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(elkConfig)
                                    .build());
  }
}
