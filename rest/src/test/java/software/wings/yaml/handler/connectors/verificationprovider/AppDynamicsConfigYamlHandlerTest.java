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
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AppDynamicsConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.AppDynamicsConfigYamlHandler;

import java.io.IOException;
import java.util.Arrays;

public class AppDynamicsConfigYamlHandlerTest extends BaseVerificationProviderYamlHandlerTest {
  @InjectMocks @Inject private AppDynamicsConfigYamlHandler yamlHandler;

  public static final String appdUrl = "https://wingsnfr.saas.appdynamics.com:443/controller";

  private String invalidYamlContent = "username_appd: username\n"
      + "password: amazonkms:zsj_HWfkSF-3li3W-9acHA\n"
      + "accountName: accountName\n"
      + "controllerUrl: https://wingsnfr.saas.appdynamics.com:443/controller\n"
      + "type: APP_DYNAMICS";

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String appdProviderName = "Appdynamics" + System.currentTimeMillis();

    // 1. Create Appdynamics verification record
    SettingAttribute settingAttributeSaved = createAppdynamicsVerificationProvider(appdProviderName);
    assertEquals(appdProviderName, settingAttributeSaved.getName());

    String yamlFilePath = getYamlFilePath(appdProviderName);
    SettingAttribute fetchedSettingAttribute = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNotNull(fetchedSettingAttribute);
    verify(settingAttributeSaved, fetchedSettingAttribute);

    // 2. update appdUrl and get Yalm String from SettingAttribute
    ((AppDynamicsConfig) settingAttributeSaved.getValue()).setControllerUrl(appdUrl + "_1");
    String yamlContent = getYamlContentString(settingAttributeSaved, yamlHandler);

    // 3. call upsert() using this yamlContent
    ChangeContext<Yaml> changeContext = getChangeContext(yamlContent, yamlFilePath, Yaml.class, yamlHandler);
    SettingAttribute settingAttributeFromYaml = yamlHandler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    verify(settingAttributeSaved, settingAttributeFromYaml);

    // 4. delete Appdynamics verification provider record using yaml
    yamlHandler.delete(changeContext);
    SettingAttribute settingAttributeDeleted = yamlHandler.get(ACCOUNT_ID, yamlFilePath);
    assertNull(settingAttributeDeleted);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String appdProviderName = "Appdynamics" + System.currentTimeMillis();

    // 1. Create appdynamics verification provider record
    SettingAttribute settingAttributeSaved = createAppdynamicsVerificationProvider(appdProviderName);
    String yamlFilePath = getYamlFilePath(appdProviderName);

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

  private SettingAttribute createAppdynamicsVerificationProvider(String appdProviderName) {
    // Generate appdynamics verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(appdProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(AppDynamicsConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .controllerUrl(appdUrl)
                                                   .username(userName)
                                                   .password(password.toCharArray())
                                                   .accountname(accountName)
                                                   .build())
                                    .build());
  }
}
