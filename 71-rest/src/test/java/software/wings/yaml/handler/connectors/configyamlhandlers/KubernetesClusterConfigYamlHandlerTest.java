package software.wings.yaml.handler.connectors.configyamlhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.HarnessException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.KubernetesClusterConfigYamlHandler;

import java.io.IOException;

public class KubernetesClusterConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private KubernetesClusterConfigYamlHandler yamlHandler;
  private static final String masterUrl = "dummyMasterUrl";
  public static final String username = "dummyUsername";
  public static final String password = "dummyPassword";

  private Class yamlClass = KubernetesClusterConfig.Yaml.class;

  @Test
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    String kubernetesClusterConfigName = "KubernetesCluster-" + System.currentTimeMillis();

    SettingAttribute settingAttributeSaved = createKubernetesClusterConfigProvider(kubernetesClusterConfigName);
    assertThat(settingAttributeSaved.getName()).isEqualTo(kubernetesClusterConfigName);

    testCRUD(generateSettingValueYamlConfig(kubernetesClusterConfigName, settingAttributeSaved));
  }

  private SettingAttribute createKubernetesClusterConfigProvider(String kubernetesClusterConfigName) {
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(SettingCategory.CLOUD_PROVIDER)
                                    .withName(kubernetesClusterConfigName)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(KubernetesClusterConfig.builder()
                                                   .masterUrl(masterUrl)
                                                   .username(username)
                                                   .password(password.toCharArray())
                                                   .accountId(ACCOUNT_ID)
                                                   .build())
                                    .build());
  }

  private SettingValueYamlConfig generateSettingValueYamlConfig(String name, SettingAttribute settingAttributeSaved) {
    return SettingValueYamlConfig.builder()
        .yamlHandler(yamlHandler)
        .yamlClass(yamlClass)
        .settingAttributeSaved(settingAttributeSaved)
        .yamlDirPath(cloudProviderYamlDir)
        .name(name)
        .configclazz(KubernetesClusterConfig.class)
        .updateMethodName("setMasterUrl")
        .currentFieldValue(masterUrl)
        .build();
  }
}
