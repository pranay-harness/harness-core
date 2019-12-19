package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.GLOBAL_APP_ID;
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
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.handler.setting.artifactserver.AmazonS3HelmRepoConfigYamlHandler;

import java.io.IOException;

public class AmazonS3HelmRepoConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private AmazonS3HelmRepoConfigYamlHandler yamlHandler;
  @InjectMocks @Inject private WingsPersistence wingsPersistence;

  private Class yamlClass = AmazonS3HelmRepoConfig.Yaml.class;
  private static final String AMAZONS3_HELM_CHART_SETTING_NAME = "AmazonS3-Helm-Repo";
  private static final String CLOUD_PROVIDER_NAME = "Amazon-Cloud-Provider";
  private static final String CLOUD_PROVIDER_ID = "Amazon-Cloud-Provider-Id";

  private String invalidYamlContent = "cloudProviderId: CONNECTOR_ID\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: AMAZON_S3_HELM_REPO\n"
      + "bucket: harness-helm-test-repo\n"
      + "folderPath: abc-1\n"
      + "region: us-east-1\n";

  @Before
  public void setup() {
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withName(CLOUD_PROVIDER_NAME)
                                            .withAccountId(ACCOUNT_ID)
                                            .withUuid(CLOUD_PROVIDER_ID)
                                            .withCategory(SettingCategory.CLOUD_PROVIDER)
                                            .withAppId(GLOBAL_APP_ID)
                                            .withValue(AwsConfig.builder()
                                                           .accountId(ACCOUNT_ID)
                                                           .accessKey("accessKey")
                                                           .secretKey("secret".toCharArray())
                                                           .build())
                                            .build();
    wingsPersistence.save(settingAttribute);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    String amazonS3HelmRepoSettingName = AMAZONS3_HELM_CHART_SETTING_NAME + System.currentTimeMillis();

    SettingAttribute settingAttributeSaved = createAmazonS3HelmRepoConnector(amazonS3HelmRepoSettingName);
    assertThat(settingAttributeSaved.getName()).isEqualTo(amazonS3HelmRepoSettingName);

    testCRUD(generateSettingValueYamlConfig(amazonS3HelmRepoSettingName, settingAttributeSaved));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testFailures() throws HarnessException, IOException {
    String amazonS3HelmRepoSettingName = AMAZONS3_HELM_CHART_SETTING_NAME + System.currentTimeMillis();

    SettingAttribute settingAttributeSaved = createAmazonS3HelmRepoConnector(amazonS3HelmRepoSettingName);
    testFailureScenario(generateSettingValueYamlConfig(amazonS3HelmRepoSettingName, settingAttributeSaved));
  }

  private SettingAttribute createAmazonS3HelmRepoConnector(String settingName) {
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(SettingCategory.HELM_REPO)
                                    .withName(settingName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(AmazonS3HelmRepoConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .bucketName("ABC")
                                                   .region("abc")
                                                   .connectorId(CLOUD_PROVIDER_ID)
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
        .configclazz(AmazonS3HelmRepoConfig.class)
        .build();
  }
}
