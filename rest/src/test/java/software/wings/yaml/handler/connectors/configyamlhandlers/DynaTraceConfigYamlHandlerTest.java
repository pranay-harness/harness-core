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
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.verificationprovider.DynaTraceConfigYamlHandler;

import java.io.IOException;

/**
 * Created by rsingh on 2/12/18.
 */
public class DynaTraceConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private DynaTraceConfigYamlHandler yamlHandler;

  public static final String url = "https://bdv73347.live.dynatrace.com";

  private String invalidYamlContent = "apiToken: amazonkms:C7cBDpxHQzG5rv30tvZDgw\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: DYNA_TRACE";

  private Class yamlClass = DynaTraceConfig.DynaTraceYaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String dynatraceProviderName = "dynaTrace" + System.currentTimeMillis();

    // 1. Create dynatrace verification record
    SettingAttribute settingAttributeSaved = createDynaTraceProviderNameVerificationProvider(dynatraceProviderName);
    assertEquals(dynatraceProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(dynatraceProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String dynatraceProviderName = "dynaTrace" + System.currentTimeMillis();

    // 1. Create dynatrace verification provider record
    SettingAttribute settingAttributeSaved = createDynaTraceProviderNameVerificationProvider(dynatraceProviderName);
    testFailureScenario(generateSettingValueYamlConfig(dynatraceProviderName, settingAttributeSaved));
  }

  private SettingAttribute createDynaTraceProviderNameVerificationProvider(String dynaTraceProviderName) {
    // Generate dynatrace verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(dynaTraceProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(DynaTraceConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .dynaTraceUrl(url)
                                                   .apiToken(apiKey.toCharArray())
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
        .configclazz(DynaTraceConfig.class)
        .updateMethodName(null)
        .currentFieldValue(null)
        .build();
  }
}
