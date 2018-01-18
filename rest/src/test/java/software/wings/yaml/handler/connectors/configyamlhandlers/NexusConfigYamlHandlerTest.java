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
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.artifactserver.NexusConfigYamlHandler;

import java.io.IOException;

public class NexusConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private NexusConfigYamlHandler yamlHandler;

  public static final String url = "https://nexus.wings.software/";

  private String invalidYamlContent = "url_Nexus: https://nexus.wings.software\n"
      + "username: admin\n"
      + "password: safeharness:Q1ESI1KVTrCaBARuR38kqA\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: NEXUS";

  private Class yamlClass = NexusConfig.Yaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String nexusProviderName = "Nexus" + System.currentTimeMillis();

    // 1. Create Nexus verification record
    SettingAttribute settingAttributeSaved = createNexusVerificationProvider(nexusProviderName);
    assertEquals(nexusProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(nexusProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String nexusProviderName = "Nexus" + System.currentTimeMillis();

    // 1. Create Nexus verification provider record
    SettingAttribute settingAttributeSaved = createNexusVerificationProvider(nexusProviderName);
    testFailureScenario(generateSettingValueYamlConfig(nexusProviderName, settingAttributeSaved));
  }

  private SettingAttribute createNexusVerificationProvider(String nexusProviderName) {
    // Generate Nexus verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(nexusProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(NexusConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .nexusUrl(url)
                                                   .username(userName)
                                                   .password(password.toCharArray())
                                                   .build())
                                    .build());
  }

  private SettingValueYamlConfig generateSettingValueYamlConfig(String name, SettingAttribute settingAttributeSaved) {
    return SettingValueYamlConfig.builder()
        .yamlHandler(yamlHandler)
        .yamlClass(yamlClass)
        .settingAttributeSaved(settingAttributeSaved)
        .yamlDirPath(artifactServerYamlDir)
        .invalidYamlContent(invalidYamlContent)
        .name(name)
        .configclazz(NexusConfig.class)
        .updateMethodName("setNexusUrl")
        .currentFieldValue(url)
        .build();
  }
}
