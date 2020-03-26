package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.impl.yaml.handler.setting.artifactserver.DockerRegistryConfigYamlHandler;

public class DockerRegistryConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private DockerRegistryConfigYamlHandler yamlHandler;

  public static final String url = "https://registry.hub.docker.com/v2/";

  private String invalidYamlContent = "url_Docker: https://registry.hub.docker.com/v2/\n"
      + "username: wingsplugins\n"
      + "password: safeharness:pUnshzJMSJuOlNIusxanfw\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: DOCKER";

  private Class yamlClass = DockerConfig.Yaml.class;

  @Before
  public void setUp() throws Exception {}

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws Exception {
    String bambooProviderName = "Docker" + System.currentTimeMillis();

    // 1. Create Docker verification record
    SettingAttribute settingAttributeSaved = createDockerVerificationProvider(bambooProviderName);
    assertThat(settingAttributeSaved.getName()).isEqualTo(bambooProviderName);

    testCRUD(generateSettingValueYamlConfig(bambooProviderName, settingAttributeSaved));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFailures() throws Exception {
    String bambooProviderName = "Docker" + System.currentTimeMillis();

    // 1. Create Docker verification provider record
    SettingAttribute settingAttributeSaved = createDockerVerificationProvider(bambooProviderName);
    testFailureScenario(generateSettingValueYamlConfig(bambooProviderName, settingAttributeSaved));
  }

  private SettingAttribute createDockerVerificationProvider(String docketRegistryName) {
    // Generate appdynamics verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(SettingCategory.CONNECTOR)
                                    .withName(docketRegistryName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(DockerConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .dockerRegistryUrl(url)
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
        .configclazz(DockerConfig.class)
        .updateMethodName("setDockerRegistryUrl")
        .currentFieldValue(url)
        .build();
  }
}
