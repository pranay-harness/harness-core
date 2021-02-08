package software.wings.service.impl.deployment.checks;

import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.exception.DeploymentFreezeException;
import io.harness.ff.FeatureFlagService;
import io.harness.governance.AllEnvFilter;
import io.harness.governance.BlackoutWindowFilterType;
import io.harness.governance.CustomAppFilter;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.resources.stats.model.TimeRange;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@TargetModule(Module._960_API_SERVICES)
public class DeploymentFreezeCheckerTest extends WingsBaseTest {
  public static final String FREEZE_ID = "FREEZE_ID";
  @Mock EnvironmentService environmentService;
  @Mock FeatureFlagService featureFlagService;
  @Mock GovernanceConfigService governanceConfigService;

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void checkEnvFrozenByMultipleWindows() {
    GovernanceConfig governanceConfig = generateGovernanceConfig();

    Map<String, Set<String>> frozenEnvs = new HashMap<>();
    frozenEnvs.put(FREEZE_ID, ImmutableSet.of(ENV_ID, ENV_ID + 2));
    frozenEnvs.put(FREEZE_ID + 2, ImmutableSet.of(ENV_ID + 3, ENV_ID + 2));
    frozenEnvs.put(FREEZE_ID + 3, ImmutableSet.of(ENV_ID + 4));
    DeploymentFreezeChecker deploymentFreezeChecker = new DeploymentFreezeChecker(
        governanceConfigService, new DeploymentCtx(APP_ID, asList(ENV_ID + 2)), environmentService, featureFlagService);
    when(governanceConfigService.getFrozenEnvIdsForApp(ACCOUNT_ID, APP_ID, governanceConfig)).thenReturn(frozenEnvs);
    assertThatThrownBy(() -> deploymentFreezeChecker.checkIfEnvFrozen(ACCOUNT_ID, governanceConfig))
        .isInstanceOf(DeploymentFreezeException.class)
        .hasMessage(
            "Deployment Freeze Windows [FREEZE2, FREEZE1] are active for the environment. No deployments are allowed to proceed.")
        .extracting("deploymentFreezeIds", InstanceOfAssertFactories.ITERABLE)
        .containsExactlyInAnyOrder(FREEZE_ID, FREEZE_ID + 2);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void checkEnvFrozenBySingleWindow() {
    GovernanceConfig governanceConfig = generateGovernanceConfig();
    Map<String, Set<String>> frozenEnvs = new HashMap<>();
    frozenEnvs.put(FREEZE_ID, ImmutableSet.of(ENV_ID, ENV_ID + 2));
    frozenEnvs.put(FREEZE_ID + 2, ImmutableSet.of(ENV_ID + 3, ENV_ID + 2));
    frozenEnvs.put(FREEZE_ID + 3, ImmutableSet.of(ENV_ID + 4));

    DeploymentFreezeChecker deploymentFreezeChecker = new DeploymentFreezeChecker(
        governanceConfigService, new DeploymentCtx(APP_ID, asList(ENV_ID + 3)), environmentService, featureFlagService);
    when(governanceConfigService.getFrozenEnvIdsForApp(ACCOUNT_ID, APP_ID, governanceConfig)).thenReturn(frozenEnvs);
    assertThatThrownBy(() -> deploymentFreezeChecker.checkIfEnvFrozen(ACCOUNT_ID, governanceConfig))
        .isInstanceOf(DeploymentFreezeException.class)
        .hasMessage(
            "Deployment Freeze Window [FREEZE2] is active for the environment. No deployments are allowed to proceed.")
        .extracting("accountId")
        .isEqualTo(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void checkEnvNotFrozenByWindow() {
    GovernanceConfig governanceConfig = generateGovernanceConfig();
    Map<String, Set<String>> frozenEnvs = new HashMap<>();
    frozenEnvs.put(FREEZE_ID, ImmutableSet.of(ENV_ID, ENV_ID + 2));
    frozenEnvs.put(FREEZE_ID + 2, ImmutableSet.of(ENV_ID + 3, ENV_ID + 2));
    frozenEnvs.put(FREEZE_ID + 3, ImmutableSet.of(ENV_ID + 4));

    DeploymentFreezeChecker deploymentFreezeChecker = new DeploymentFreezeChecker(
        governanceConfigService, new DeploymentCtx(APP_ID, asList(ENV_ID + 5)), environmentService, featureFlagService);
    when(governanceConfigService.getFrozenEnvIdsForApp(ACCOUNT_ID, APP_ID, governanceConfigService.get(ACCOUNT_ID)))
        .thenReturn(frozenEnvs);
    deploymentFreezeChecker.checkIfEnvFrozen(ACCOUNT_ID, governanceConfig);
    verify(governanceConfigService, times(1)).getFrozenEnvIdsForApp(ACCOUNT_ID, APP_ID, governanceConfig);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void checkEnvNotFrozenIfNoWindowsActive() {
    GovernanceConfig governanceConfig = generateGovernanceConfig();

    Map<String, Set<String>> frozenEnvs = new HashMap<>();
    DeploymentFreezeChecker deploymentFreezeChecker = new DeploymentFreezeChecker(
        governanceConfigService, new DeploymentCtx(APP_ID, asList(ENV_ID + 5)), environmentService, featureFlagService);
    when(governanceConfigService.getFrozenEnvIdsForApp(ACCOUNT_ID, APP_ID, governanceConfigService.get(ACCOUNT_ID)))
        .thenReturn(frozenEnvs);
    deploymentFreezeChecker.checkIfEnvFrozen(ACCOUNT_ID, governanceConfig);
    verify(governanceConfigService, times(1)).getFrozenEnvIdsForApp(ACCOUNT_ID, APP_ID, governanceConfig);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void checkAppFrozenIfEnvIsEmpty() {
    GovernanceConfig governanceConfig = generateGovernanceConfig();

    Map<String, Set<String>> frozenEnvs = new HashMap<>();
    frozenEnvs.put(FREEZE_ID, ImmutableSet.of(ENV_ID, ENV_ID + 2));
    frozenEnvs.put(FREEZE_ID + 2, ImmutableSet.of(ENV_ID + 3, ENV_ID + 2));
    frozenEnvs.put(FREEZE_ID + 3, ImmutableSet.of(ENV_ID + 4));
    DeploymentFreezeChecker deploymentFreezeChecker = new DeploymentFreezeChecker(governanceConfigService,
        new DeploymentCtx(APP_ID, Collections.emptyList()), environmentService, featureFlagService);
    when(governanceConfigService.get(ACCOUNT_ID)).thenReturn(governanceConfig);
    when(featureFlagService.isEnabled(FeatureName.NEW_DEPLOYMENT_FREEZE, ACCOUNT_ID)).thenReturn(true);
    when(governanceConfigService.getFrozenEnvIdsForApp(ACCOUNT_ID, APP_ID, governanceConfig)).thenReturn(frozenEnvs);
    assertThatThrownBy(() -> deploymentFreezeChecker.check(ACCOUNT_ID))
        .isInstanceOf(DeploymentFreezeException.class)
        .hasMessage(
            "Deployment Freeze Window [FREEZE1] is active for the environment. No deployments are allowed to proceed.")
        .extracting("deploymentFreezeIds", InstanceOfAssertFactories.ITERABLE)
        .containsExactlyInAnyOrder(FREEZE_ID);
  }

  private GovernanceConfig generateGovernanceConfig() {
    TimeRange timeRange = new TimeRange(System.currentTimeMillis(), System.currentTimeMillis() + 100_000, "");
    TimeRangeBasedFreezeConfig freezeConfig =
        TimeRangeBasedFreezeConfig.builder()
            .applicable(true)
            .appSelections(asList(CustomAppFilter.builder()
                                      .apps(asList(APP_ID))
                                      .blackoutWindowFilterType(BlackoutWindowFilterType.CUSTOM)
                                      .envSelection(new AllEnvFilter(EnvironmentFilterType.ALL))
                                      .build()))
            .name("FREEZE1")
            .timeRange(timeRange)
            .build();
    freezeConfig.setUuid(FREEZE_ID);
    TimeRangeBasedFreezeConfig freezeConfig2 =
        TimeRangeBasedFreezeConfig.builder().name("FREEZE2").timeRange(timeRange).build();
    freezeConfig2.setUuid(FREEZE_ID + 2);
    TimeRangeBasedFreezeConfig freezeConfig3 =
        TimeRangeBasedFreezeConfig.builder().name("FREEZE3").timeRange(timeRange).build();
    freezeConfig3.setUuid(FREEZE_ID + 3);
    return GovernanceConfig.builder()
        .deploymentFreeze(false)
        .timeRangeBasedFreezeConfigs(asList(freezeConfig, freezeConfig2, freezeConfig3))
        .build();
  }
}