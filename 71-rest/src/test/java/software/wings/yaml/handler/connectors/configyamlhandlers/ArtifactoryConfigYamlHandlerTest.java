package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.HarnessException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.config.ArtifactoryConfig;
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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    String artifactoryProviderName = "Artifactory" + System.currentTimeMillis();

    // 1. Create Artifactory verification record
    SettingAttribute settingAttributeSaved = createArtifactoryVerificationProvider(artifactoryProviderName);
    assertThat(settingAttributeSaved.getName()).isEqualTo(artifactoryProviderName);

    testCRUD(generateSettingValueYamlConfig(artifactoryProviderName, settingAttributeSaved));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
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
                                    .withCategory(SettingCategory.CONNECTOR)
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
