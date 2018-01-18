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
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.artifactserver.ArtifactoryConfigYamlHandler;

import java.io.IOException;

public class ArtifactoryConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private ArtifactoryConfigYamlHandler yamlHandler;

  public static final String url = "https://harness.jfrog.io/harness";

  private String invalidYamlContent = "url_invalid: https://harness.jfrog.io/harness\n"
      + "username: admin\n"
      + "password: safeharness:JAhmPyeCQYaVVRO4YULw6A\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: ARTIFACTORY";

  private Class yamlClass = ArtifactoryConfig.Yaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {}

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String artifactoryProviderName = "Artifactory" + System.currentTimeMillis();

    // 1. Create Artifactory verification record
    SettingAttribute settingAttributeSaved = createArtifactoryVerificationProvider(artifactoryProviderName);
    assertEquals(artifactoryProviderName, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(artifactoryProviderName, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String artifactoryProviderName = "Artifactory" + System.currentTimeMillis();

    // 1. Create Artifactory verification provider record
    SettingAttribute settingAttributeSaved = createArtifactoryVerificationProvider(artifactoryProviderName);
    testFailureScenario(generateSettingValueYamlConfig(artifactoryProviderName, settingAttributeSaved));
  }

  private SettingAttribute createArtifactoryVerificationProvider(String ArtifactoryProviderName) {
    // Generate Artifactory verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(ArtifactoryProviderName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(ArtifactoryConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .artifactoryUrl(url)
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
        .configclazz(ArtifactoryConfig.class)
        .updateMethodName("setArtifactoryUrl")
        .currentFieldValue(url)
        .build();
  }
}
