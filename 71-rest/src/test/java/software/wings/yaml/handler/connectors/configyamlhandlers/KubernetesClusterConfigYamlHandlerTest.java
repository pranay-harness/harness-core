package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.PUNEET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.CCMConfigYamlHandler;
import io.harness.ccm.CCMSettingService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.OidcGrantType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.impl.yaml.handler.setting.cloudprovider.KubernetesClusterConfigYamlHandler;

public class KubernetesClusterConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @Mock private CCMSettingService ccmSettingService;
  @Mock private CCMConfigYamlHandler ccmConfigYamlHandler;
  @InjectMocks @Inject private KubernetesClusterConfigYamlHandler yamlHandler;
  private static final String masterUrl = "dummyMasterUrl";
  public static final String username = "dummyUsername";
  public static final String password = "dummyPassword";

  private Class yamlClass = KubernetesClusterConfig.Yaml.class;

  @Before
  public void setUp() {
    when(ccmSettingService.isCloudCostEnabled(anyString())).thenReturn(false);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws Exception {
    String kubernetesClusterConfigName = "KubernetesCluster-" + System.currentTimeMillis();

    SettingAttribute k8sClusterConfigWithoutValidation =
        createKubernetesClusterConfigProvider(kubernetesClusterConfigName, true);
    assertThat(k8sClusterConfigWithoutValidation.getName()).isEqualTo(kubernetesClusterConfigName);

    testCRUD(generateSettingValueYamlConfig(kubernetesClusterConfigName, k8sClusterConfigWithoutValidation));

    SettingAttribute K8sClusterConfigWithValidation =
        createKubernetesClusterConfigProvider(kubernetesClusterConfigName, false);
    assertThat(K8sClusterConfigWithValidation.getName()).isEqualTo(kubernetesClusterConfigName);

    testCRUD(generateSettingValueYamlConfig(kubernetesClusterConfigName, K8sClusterConfigWithValidation));

    SettingAttribute oidcCloudProvider = createKubernetesClusterConfigOIDCProvider(kubernetesClusterConfigName, false);
    assertThat(oidcCloudProvider.getName()).isEqualTo(kubernetesClusterConfigName);

    testCRUD(generateSettingValueYamlConfig(kubernetesClusterConfigName, oidcCloudProvider));
  }

  private SettingAttribute createKubernetesClusterConfigProvider(
      String kubernetesClusterConfigName, boolean skipValidation) {
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
                                                   .skipValidation(skipValidation)
                                                   .build())
                                    .build());
  }

  private SettingAttribute createKubernetesClusterConfigOIDCProvider(
      String kubernetesClusterConfigName, boolean skipValidation) {
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withName(kubernetesClusterConfigName)
                                    .withCategory(SettingCategory.CLOUD_PROVIDER)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(KubernetesClusterConfig.builder()
                                                   .masterUrl(masterUrl)
                                                   .oidcUsername(username)
                                                   .oidcPassword(password.toCharArray())
                                                   .accountId(ACCOUNT_ID)
                                                   .oidcIdentityProviderUrl("oidcUrl")
                                                   .oidcClientId("clientId".toCharArray())
                                                   .oidcSecret("secret".toCharArray())
                                                   .oidcScopes("email")
                                                   .oidcGrantType(OidcGrantType.password)
                                                   .skipValidation(skipValidation)
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
