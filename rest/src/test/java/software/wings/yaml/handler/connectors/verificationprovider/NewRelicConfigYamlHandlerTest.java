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
import software.wings.beans.NewRelicConfig;
import software.wings.beans.NewRelicConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.NewRelicConfigYamlHandler;

import java.io.IOException;
import java.util.Arrays;

public class NewRelicConfigYamlHandlerTest extends BaseVerificationProviderYamlHandlerTest {
  @InjectMocks @Inject private NewRelicConfigYamlHandler yamlHandler;

  public static final String newRelicUrl = "https://api.newrelic.com";

  private String invalidYamlContent = "apiKey_newrelic: amazonkms:C7cBDpxHQzG5rv30tvZDgw\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: NEW_RELIC";

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String newRelicProviderName = "newRelic" + System.currentTimeMillis();

    // 1. Create newRelic verification record
    SettingAttribute settingAttributeSaved = createNewRelicProviderNameVerificationProvider(newRelicProviderName);
    assertEquals(newRelicProviderName, settingAttributeSaved.getName());
    String yamlFilePath = getYamlFilePath(newRelicProviderName);
    SettingAttribute fetchedSettingAttribute = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    verify(settingAttributeSaved, fetchedSettingAttribute);

    // 2. Get Yalm String from SettingAttribute
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);

    // 3. call upsert() using this yamlContent
    ChangeContext<Yaml> changeContext = getChangeContext(yamlContent, yamlFilePath, Yaml.class, yamlHandler);
    SettingAttribute settingAttributeFromYaml = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    verify(settingAttributeSaved, settingAttributeFromYaml);

    // 4. delete newRelic verification provider record using yaml
    yamlHandler.delete(changeContext);
    SettingAttribute settingAttributeDeleted = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNull(settingAttributeDeleted);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String newRelicProviderName = "newRelic" + System.currentTimeMillis();

    // 1. Create newRelic verification record
    SettingAttribute settingAttributeSaved = createNewRelicProviderNameVerificationProvider(newRelicProviderName);
    String validYamlFilePath = getYamlFilePath(newRelicProviderName);

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

  private SettingAttribute createNewRelicProviderNameVerificationProvider(String newRelicProviderName) {
    // Generate newRelic verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(newRelicProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(NewRelicConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .newRelicUrl(newRelicUrl)
                                                   .apiKey(apiKey.toCharArray())
                                                   .build())
                                    .build());
  }
}
