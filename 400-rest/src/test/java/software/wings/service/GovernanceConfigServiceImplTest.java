package software.wings.service;

import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.beans.EnvironmentType;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.governance.AllAppFilter;
import io.harness.governance.AllEnvFilter;
import io.harness.governance.AllProdEnvFilter;
import io.harness.governance.ApplicationFilter;
import io.harness.governance.BlackoutWindowFilterType;
import io.harness.governance.CustomAppFilter;
import io.harness.governance.CustomEnvFilter;
import io.harness.governance.DeploymentFreezeInfo;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.resources.stats.model.TimeRange;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class GovernanceConfigServiceImplTest extends WingsBaseTest {
  @Mock FeatureFlagService featureFlagService;
  @Mock EnvironmentService environmentService;
  @Mock AppService appService;
  @Inject @InjectMocks private GovernanceConfigService governanceConfigService;

  @Before
  public void setUp() {
    when(featureFlagService.isEnabled(eq(FeatureName.NEW_DEPLOYMENT_FREEZE), anyString())).thenReturn(true);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldSaveAndGetDeploymentFreeze() {
    GovernanceConfig governanceConfig =
        JsonUtils.readResourceFile("./governance/governance_config.json", GovernanceConfig.class);

    GovernanceConfig savedGovernanceConfig =
        governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig);
    savedGovernanceConfig.setUuid("GOVERNANCE_CONFIG_ID");
    JsonNode actual = JsonUtils.toJsonNode(savedGovernanceConfig);
    JsonNode expected = JsonUtils.readResourceFile("./governance/governance_config_expected.json", JsonNode.class);
    assertThat(actual).hasToString(expected.toString());
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForCustomEnvWithNonCustomApp() {
    GovernanceConfig governanceConfig =
        JsonUtils.readResourceFile("./governance/governance_config.json", GovernanceConfig.class);
    governanceConfig.getTimeRangeBasedFreezeConfigs().get(0).getAppSelections().get(0).setFilterType(
        BlackoutWindowFilterType.ALL);

    assertThatThrownBy(() -> governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Environment filter type can be CUSTOM only when Application Filter type is CUSTOM");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForCustomEnvWithMultipleApps() {
    GovernanceConfig governanceConfig =
        JsonUtils.readResourceFile("./governance/governance_config.json", GovernanceConfig.class);
    CustomAppFilter appConfig =
        (CustomAppFilter) governanceConfig.getTimeRangeBasedFreezeConfigs().get(0).getAppSelections().get(0);
    appConfig.getApps().add("app2");

    assertThatThrownBy(() -> governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Application filter should have exactly one app when environment filter type is CUSTOM");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldGetFrozenAppEnvs() {
    TimeRange range =
        new TimeRange(System.currentTimeMillis(), System.currentTimeMillis() + 1_000_000, "Asia/Kolkatta");

    ApplicationFilter appSelection1 = AllAppFilter.builder()
                                          .blackoutWindowFilterType(BlackoutWindowFilterType.ALL)
                                          .envSelection(new AllProdEnvFilter(EnvironmentFilterType.ALL_PROD))
                                          .build();

    ApplicationFilter appSelection2 = CustomAppFilter.builder()
                                          .blackoutWindowFilterType(BlackoutWindowFilterType.CUSTOM)
                                          .apps(asList(APP_ID, APP_ID + 2))
                                          .envSelection(new AllEnvFilter(EnvironmentFilterType.ALL))
                                          .build();

    ApplicationFilter appSelection3 =
        CustomAppFilter.builder()
            .blackoutWindowFilterType(BlackoutWindowFilterType.CUSTOM)
            .apps(asList(APP_ID + 3))
            .envSelection(new CustomEnvFilter(EnvironmentFilterType.CUSTOM, asList(ENV_ID, ENV_ID + 2)))
            .build();
    TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig =
        new TimeRangeBasedFreezeConfig(true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD),
            range, null, null, true, asList(appSelection1, appSelection2, appSelection3), null);
    GovernanceConfig governanceConfig =
        GovernanceConfig.builder()
            .accountId(ACCOUNT_ID)
            .timeRangeBasedFreezeConfigs(Collections.singletonList(timeRangeBasedFreezeConfig))
            .build();

    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(asList(APP_ID, APP_ID + 2, APP_ID + 3));
    Map<String, Set<String>> prodEnvs = ImmutableMap.<String, Set<String>>builder()
                                            .put(APP_ID + 2, new HashSet<>(asList(ENV_ID)))
                                            .put(APP_ID + 3, new HashSet<>(asList(ENV_ID + 3)))
                                            .build();
    when(environmentService.getAppIdEnvIdMapByType(anySet(), eq(EnvironmentType.PROD))).thenReturn(prodEnvs);
    governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig);

    DeploymentFreezeInfo deploymentFreezeInfo = governanceConfigService.getDeploymentFreezeInfo(ACCOUNT_ID);
    assertThat(deploymentFreezeInfo).isNotNull();
    assertThat(deploymentFreezeInfo.isFreezeAll()).isFalse();
    assertThat(deploymentFreezeInfo.getAppEnvs()).hasSize(2);
    assertThat(deploymentFreezeInfo.getAllEnvFrozenApps()).containsExactlyInAnyOrder(APP_ID, APP_ID + 2);
    assertThat(deploymentFreezeInfo.getAppEnvs().get(APP_ID + 2)).containsExactlyInAnyOrder(ENV_ID);
    assertThat(deploymentFreezeInfo.getAppEnvs().get(APP_ID + 3))
        .containsExactlyInAnyOrder(ENV_ID + 3, ENV_ID, ENV_ID + 2);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldGetFrozenAllAppEnvs() {
    TimeRange range = new TimeRange(System.currentTimeMillis(), System.currentTimeMillis() + 100_000, "Asia/Kolkatta");

    ApplicationFilter appSelection1 = AllAppFilter.builder()
                                          .blackoutWindowFilterType(BlackoutWindowFilterType.ALL)
                                          .envSelection(new AllEnvFilter(EnvironmentFilterType.ALL))
                                          .build();

    ApplicationFilter appSelection3 =
        CustomAppFilter.builder()
            .blackoutWindowFilterType(BlackoutWindowFilterType.CUSTOM)
            .apps(asList(APP_ID))
            .envSelection(new CustomEnvFilter(EnvironmentFilterType.CUSTOM, asList(ENV_ID, ENV_ID + 2)))
            .build();
    TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig =
        new TimeRangeBasedFreezeConfig(true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD),
            range, null, null, true, asList(appSelection1, appSelection3), null);
    GovernanceConfig governanceConfig =
        GovernanceConfig.builder()
            .accountId(ACCOUNT_ID)
            .timeRangeBasedFreezeConfigs(Collections.singletonList(timeRangeBasedFreezeConfig))
            .build();

    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(asList(APP_ID, APP_ID + 2, APP_ID + 3));
    governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig);

    DeploymentFreezeInfo deploymentFreezeInfo = governanceConfigService.getDeploymentFreezeInfo(ACCOUNT_ID);
    assertThat(deploymentFreezeInfo).isNotNull();
    assertThat(deploymentFreezeInfo.isFreezeAll()).isFalse();
    assertThat(deploymentFreezeInfo.getAppEnvs()).hasSize(1);
    assertThat(deploymentFreezeInfo.getAllEnvFrozenApps()).containsExactlyInAnyOrder(APP_ID, APP_ID + 2, APP_ID + 3);
    assertThat(deploymentFreezeInfo.getAppEnvs().get(APP_ID)).containsExactlyInAnyOrder(ENV_ID, ENV_ID + 2);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldGetOnlyApplicableAppEnvs() {
    TimeRange range = new TimeRange(System.currentTimeMillis(), System.currentTimeMillis() + 100_000, "Asia/Kolkatta");

    ApplicationFilter appSelection1 = AllAppFilter.builder()
                                          .blackoutWindowFilterType(BlackoutWindowFilterType.ALL)
                                          .envSelection(new AllEnvFilter(EnvironmentFilterType.ALL))
                                          .build();

    ApplicationFilter appSelection3 =
        CustomAppFilter.builder()
            .blackoutWindowFilterType(BlackoutWindowFilterType.CUSTOM)
            .apps(asList(APP_ID))
            .envSelection(new CustomEnvFilter(EnvironmentFilterType.CUSTOM, asList(ENV_ID, ENV_ID + 2)))
            .build();
    TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig =
        new TimeRangeBasedFreezeConfig(true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD),
            range, null, null, false, asList(appSelection1, appSelection3), null);

    TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig2 =
        new TimeRangeBasedFreezeConfig(true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD),
            range, null, null, true, asList(appSelection3), null);
    GovernanceConfig governanceConfig =
        GovernanceConfig.builder()
            .accountId(ACCOUNT_ID)
            .timeRangeBasedFreezeConfigs(asList(timeRangeBasedFreezeConfig, timeRangeBasedFreezeConfig2))
            .build();

    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(asList(APP_ID, APP_ID + 2, APP_ID + 3));
    governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig);

    DeploymentFreezeInfo deploymentFreezeInfo = governanceConfigService.getDeploymentFreezeInfo(ACCOUNT_ID);
    assertThat(deploymentFreezeInfo).isNotNull();
    assertThat(deploymentFreezeInfo.isFreezeAll()).isFalse();
    assertThat(deploymentFreezeInfo.getAppEnvs()).hasSize(1);
    assertThat(deploymentFreezeInfo.getAppEnvs().get(APP_ID)).containsExactlyInAnyOrder(ENV_ID, ENV_ID + 2);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnEmptyMapForNoAppSelections() {
    TimeRange range = new TimeRange(System.currentTimeMillis(), System.currentTimeMillis() + 100_000, "Asia/Kolkatta");

    TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig =
        new TimeRangeBasedFreezeConfig(true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD),
            range, null, null, false, Collections.emptyList(), null);

    GovernanceConfig governanceConfig = GovernanceConfig.builder()
                                            .accountId(ACCOUNT_ID)
                                            .timeRangeBasedFreezeConfigs(asList(timeRangeBasedFreezeConfig))
                                            .build();

    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(asList(APP_ID, APP_ID + 2, APP_ID + 3));

    governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig);
    DeploymentFreezeInfo deploymentFreezeInfo = governanceConfigService.getDeploymentFreezeInfo(ACCOUNT_ID);
    assertThat(deploymentFreezeInfo).isNotNull();
    assertThat(deploymentFreezeInfo.isFreezeAll()).isFalse();
    assertThat(deploymentFreezeInfo.getAppEnvs()).isEmpty();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnEmptyMapForNoWindows() {
    GovernanceConfig governanceConfig = GovernanceConfig.builder()
                                            .accountId(ACCOUNT_ID)
                                            .deploymentFreeze(true)
                                            .timeRangeBasedFreezeConfigs(Collections.emptyList())
                                            .build();

    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(asList(APP_ID, APP_ID + 2, APP_ID + 3));

    governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig);
    DeploymentFreezeInfo deploymentFreezeInfo = governanceConfigService.getDeploymentFreezeInfo(ACCOUNT_ID);
    assertThat(deploymentFreezeInfo).isNotNull();
    assertThat(deploymentFreezeInfo.isFreezeAll()).isTrue();
    assertThat(deploymentFreezeInfo.getAppEnvs()).isEmpty();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnEmptyMapForOutofTimeRange() {
    TimeRange range = new TimeRange(100, 200, "Asia/Kolkatta");

    ApplicationFilter appSelection3 =
        CustomAppFilter.builder()
            .blackoutWindowFilterType(BlackoutWindowFilterType.CUSTOM)
            .apps(asList(APP_ID))
            .envSelection(new CustomEnvFilter(EnvironmentFilterType.CUSTOM, asList(ENV_ID, ENV_ID + 2)))
            .build();

    TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig =
        new TimeRangeBasedFreezeConfig(true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD),
            range, null, null, false, asList(appSelection3), null);

    GovernanceConfig governanceConfig = GovernanceConfig.builder()
                                            .accountId(ACCOUNT_ID)
                                            .timeRangeBasedFreezeConfigs(asList(timeRangeBasedFreezeConfig))
                                            .build();

    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(asList(APP_ID, APP_ID + 2, APP_ID + 3));

    governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig);
    DeploymentFreezeInfo deploymentFreezeInfo = governanceConfigService.getDeploymentFreezeInfo(ACCOUNT_ID);
    assertThat(deploymentFreezeInfo).isNotNull();
    assertThat(deploymentFreezeInfo.isFreezeAll()).isFalse();
    assertThat(deploymentFreezeInfo.getAppEnvs()).isEmpty();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForDuplicateNames() {
    GovernanceConfig governanceConfig =
        JsonUtils.readResourceFile("./governance/governance_config.json", GovernanceConfig.class);
    TimeRangeBasedFreezeConfig freeze1 = governanceConfig.getTimeRangeBasedFreezeConfigs().get(0);
    governanceConfig.getTimeRangeBasedFreezeConfigs().add(freeze1);

    assertThatThrownBy(() -> governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Duplicate Deployment Freeze name freezer found.");
  }
}
