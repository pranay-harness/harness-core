package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.HarnessException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.service.impl.yaml.handler.setting.artifactserver.AzureArtifactsPATConfigYamlHandler;

import java.io.IOException;

public class AzureArtifactsPATConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private AzureArtifactsPATConfigYamlHandler yamlHandler;

  private Class yamlClass = AzureArtifactsPATConfig.Yaml.class;
  private static final String azureDevopsUrl = "http://dev.azure.com/garvit-test";
  private static final String AZURE_ARTIFACTS_SETTING_NAME = "azure-artifacts";

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    String azureArtifactsSettingName = AZURE_ARTIFACTS_SETTING_NAME + System.currentTimeMillis();
    SettingAttribute settingAttributeSaved = createAzureArtifactsConnector(azureArtifactsSettingName);
    assertThat(settingAttributeSaved.getName()).isEqualTo(azureArtifactsSettingName);
    testCRUD(generateSettingValueYamlConfig(azureArtifactsSettingName, settingAttributeSaved));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFailures() throws HarnessException, IOException {
    String azureArtifactsSettingName = AZURE_ARTIFACTS_SETTING_NAME + System.currentTimeMillis();
    SettingAttribute settingAttributeSaved = createAzureArtifactsConnector(azureArtifactsSettingName);
    testFailureScenario(generateSettingValueYamlConfig(azureArtifactsSettingName, settingAttributeSaved));
  }

  private SettingAttribute createAzureArtifactsConnector(String settingName) {
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(SettingCategory.AZURE_ARTIFACTS)
                                    .withName(settingName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(AzureArtifactsPATConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .azureDevopsUrl(azureDevopsUrl)
                                                   .pat(token.toCharArray())
                                                   .build())
                                    .build());
  }

  private SettingValueYamlConfig generateSettingValueYamlConfig(String name, SettingAttribute settingAttributeSaved) {
    String invalidYamlContent = "harnessApiVersion: '1.0'\n"
        + "type: AZURE_ARTIFACTS_PAT\n"
        + "azureUrl: https://random.azure/org\n"
        + "pat: afeharness:kdT-tC2dTNCyY2pJJzSN9A";

    return SettingValueYamlConfig.builder()
        .yamlHandler(yamlHandler)
        .yamlClass(yamlClass)
        .settingAttributeSaved(settingAttributeSaved)
        .yamlDirPath(artifactServerYamlDir)
        .invalidYamlContent(invalidYamlContent)
        .name(name)
        .configclazz(AzureArtifactsPATConfig.class)
        .updateMethodName("setAzureDevopsUrl")
        .currentFieldValue(azureDevopsUrl)
        .build();
  }
}
