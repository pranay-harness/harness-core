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
import software.wings.beans.SumoConfig;
import software.wings.beans.SumoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.SumoConfigYamlHandler;

import java.io.IOException;
import java.util.Arrays;

public class SumoConfigYamlHandlerTest extends BaseVerificationProviderYamlHandlerTest {
  @InjectMocks @Inject private SumoConfigYamlHandler yamlHandler;

  public static final String sumoUrl = "https://api.us2.sumologic.com/api/v1/";

  private String invalidYamlContent = "sumoUrl_controlller: https://api.us2.sumologic.com/api/v1/\n"
      + "accessId: safeharness:pgWCawZUSnuZYi0lvgfQQQ\n"
      + "accessKey: safeharness:kdT-tC2dTNCyY2pJJzSN9A\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: SUMO\n";

  @Before
  public void setUp() throws HarnessException, IOException {}
  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String sumoProviderName = "Sumo" + System.currentTimeMillis();

    // 1. Create sumoLogic verification record
    SettingAttribute settingAttributeSaved = createSumoVerificationProvider(sumoProviderName);
    assertEquals(sumoProviderName, settingAttributeSaved.getName());
    String yamlFilePath = getYamlFilePath(sumoProviderName);
    SettingAttribute fetchedSettingAttributeSaved = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNotNull(fetchedSettingAttributeSaved);
    verify(settingAttributeSaved, fetchedSettingAttributeSaved);

    // 2. update sumoUrl and get Yalm String from SettingAttribute
    ((SumoConfig) settingAttributeSaved.getValue()).setSumoUrl(sumoUrl + "_1");
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);

    // 3. call upsert() using this yamlContent
    ChangeContext<Yaml> changeContext = getChangeContext(yamlContent, yamlFilePath, Yaml.class, yamlHandler);
    SettingAttribute settingAttributeFromYaml = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));

    // 4. delete sumoLogic verification provider record using yaml
    yamlHandler.delete(changeContext);
    SettingAttribute settingAttributeDeleted = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNull(settingAttributeDeleted);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String sumoProviderName = "Sumo" + System.currentTimeMillis();

    // 1. Create sumoLogic verification provider record
    SettingAttribute settingAttributeSaved = createSumoVerificationProvider(sumoProviderName);
    String yamlFilePath = getYamlFilePath(sumoProviderName);

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

  private SettingAttribute createSumoVerificationProvider(String sumoProviderName) {
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    SumoConfig sumoConfig = new SumoConfig();
    sumoConfig.setAccessId(acessId.toCharArray());
    sumoConfig.setAccessKey(accesskey.toCharArray());
    sumoConfig.setSumoUrl(sumoUrl);
    sumoConfig.setAccountId(ACCOUNT_ID);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(sumoProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(sumoConfig)
                                    .build());
  }
}
