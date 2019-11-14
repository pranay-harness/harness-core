package io.harness.ccm;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;

public class CCMSettingServiceTest extends WingsBaseTest {
  String accountIdWithCCM;

  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private static final String COMPANY_NAME = "Harness";
  private static final String ACCOUNT_NAME = "Harness";
  private static final String masterUrl = "dummyMasterUrl";
  public static final String username = "dummyUsername";
  public static final String password = "dummyPassword";

  private SettingAttribute settingAttribute;

  private Cluster k8sCluster;
  private ClusterRecord clusterRecord;

  @Mock AccountService accountService;
  @Mock SettingsService settingsService;
  @InjectMocks @Inject CCMSettingService ccmSettingService;

  @Before
  public void setUp() {
    accountIdWithCCM = "ACCOUNT_WITH_CCM";
    Account accountWithCCM = anAccount()
                                 .withCompanyName(COMPANY_NAME)
                                 .withAccountName(ACCOUNT_NAME)
                                 .withAccountKey("ACCOUNT_KEY")
                                 .withCloudCostEnabled(true)
                                 .build();
    when(accountService.get(eq(accountIdWithCCM))).thenReturn(accountWithCCM);

    String kubernetesClusterConfigName = "KubernetesCluster-" + System.currentTimeMillis();
    CCMConfig ccmConfig = CCMConfig.builder().cloudCostEnabled(true).build();
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder()
                                                          .masterUrl(masterUrl)
                                                          .username(username)
                                                          .password(password.toCharArray())
                                                          .accountId(accountIdWithCCM)
                                                          .ccmConfig(ccmConfig)
                                                          .build();
    settingAttribute = aSettingAttribute()
                           .withCategory(SettingCategory.CLOUD_PROVIDER)
                           .withAccountId(accountIdWithCCM)
                           .withName(kubernetesClusterConfigName)
                           .withValue(kubernetesClusterConfig)
                           .build();

    when(settingsService.get(eq(cloudProviderId))).thenReturn(settingAttribute);

    k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).build();
    clusterRecord = ClusterRecord.builder().accountId(accountIdWithCCM).cluster(k8sCluster).build();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testIsCloudCostEnabledForCloudProvider() {
    boolean result = ccmSettingService.isCloudCostEnabled(settingAttribute);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testIsCloudCostEnabledForCluster() {
    boolean result = ccmSettingService.isCloudCostEnabled(clusterRecord);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldNotMaskCCMConfig() {
    SettingAttribute maskedSettingAttribute = ccmSettingService.maskCCMConfig(settingAttribute);
    assertThat(maskedSettingAttribute).isEqualTo(settingAttribute);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldMaskCCMConfig() {
    String accountIdNoCCM = "ACCOUNT_NO_CCM";
    Account accountNoCCM = anAccount()
                               .withCompanyName(COMPANY_NAME)
                               .withAccountName(ACCOUNT_NAME)
                               .withAccountKey("ACCOUNT_KEY")
                               .withCloudCostEnabled(false)
                               .build();
    when(accountService.get(accountIdNoCCM)).thenReturn(accountNoCCM);

    String kubernetesClusterConfigName = "KubernetesCluster-" + System.currentTimeMillis();
    CCMConfig ccmConfig = CCMConfig.builder().cloudCostEnabled(true).build();
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder()
                                                          .masterUrl(masterUrl)
                                                          .username(username)
                                                          .password(password.toCharArray())
                                                          .accountId(accountIdNoCCM)
                                                          .ccmConfig(ccmConfig)
                                                          .build();
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(SettingCategory.CLOUD_PROVIDER)
                                            .withName(kubernetesClusterConfigName)
                                            .withAccountId(accountIdNoCCM)
                                            .withValue(kubernetesClusterConfig)
                                            .build();
    SettingAttribute maskedSettingAttribute = ccmSettingService.maskCCMConfig(settingAttribute);
    assertThat(maskedSettingAttribute.getValue() instanceof KubernetesClusterConfig).isTrue();
    assertThat(((KubernetesClusterConfig) maskedSettingAttribute.getValue()).getCcmConfig()).isNull();
  }
}
