package software.wings.service.impl.deployment.checks;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.data.validator.ConditionsValidator;
import io.harness.data.validator.ConditionsValidator.Condition;
import io.harness.exception.WingsException;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.service.intfc.deployment.PreDeploymentChecker;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;

@Slf4j
@ParametersAreNonnullByDefault
public class DeploymentFreezeChecker implements PreDeploymentChecker {
  private GovernanceConfigService governanceConfigService;
  private DeploymentCtx deploymentCtx;
  private EnvironmentService environmentService;

  public DeploymentFreezeChecker(GovernanceConfigService governanceConfigService, DeploymentCtx deploymentCtx,
      EnvironmentService environmentService) {
    this.governanceConfigService = governanceConfigService;
    this.deploymentCtx = deploymentCtx;
    this.environmentService = environmentService;
  }

  @Override
  public void check(String accountId) {
    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    if (governanceConfig == null) {
      return;
    }

    if (governanceConfig.isDeploymentFreeze()) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Deployment Freeze is active. No deployments are allowed.");
    }

    if (matches(deploymentCtx, governanceConfig)) {
      logger.info("Deployment Context matches governance config. accountId={} GovernanceConfig: {} Ctx: {}", accountId,
          governanceConfig, deploymentCtx);
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Deployment Freeze window is active. No deployments are allowed.");
    }
  }

  public boolean matches(DeploymentCtx deploymentCtx, GovernanceConfig freezeConfig) {
    boolean itsAMatch = freezeConfig.getTimeRangeBasedFreezeConfigs().stream().anyMatch(
        (TimeRangeBasedFreezeConfig it) -> this.matches(deploymentCtx, it));
    logger.info("DeploymentCtx: {}, governanceConfig: {}. Match: {}", deploymentCtx, freezeConfig.getUuid(), itsAMatch);
    return itsAMatch;
  }

  public boolean matches(final DeploymentCtx deploymentCtx, final TimeRangeBasedFreezeConfig freezeConfig) {
    ConditionsValidator conditions = new ConditionsValidator();

    // apps should match
    Condition condition = new Condition(
        "Freeze configuration apps match deployment context apps", () -> appsMatcher(freezeConfig, deploymentCtx));
    conditions.addCondition(condition);

    // environment Types should match
    condition = new Condition("Freeze configuration env types should match deployment env types",
        () -> envTypeMatcher(freezeConfig, deploymentCtx));
    conditions.addCondition(condition);

    // time range should match
    final long now = System.currentTimeMillis();
    condition = new Condition(
        "Freeze configuration contains current timestamp", () -> freezeConfig.getTimeRange().isInRange(now));
    conditions.addCondition(condition);

    return conditions.allConditionsSatisfied();
  }

  private Boolean envTypeMatcher(final TimeRangeBasedFreezeConfig freezeConfig, final DeploymentCtx deploymentCtx) {
    boolean envTypesIsAll = false;

    if (!freezeConfig.getEnvironmentTypes().isEmpty()) {
      envTypesIsAll = freezeConfig.getEnvironmentTypes().get(0).equals(EnvironmentType.ALL);
    }

    if (envTypesIsAll) {
      return true;
    }

    List<EnvironmentType> envTypesInDeploymentCtx =
        environmentService.getEnvByApp(deploymentCtx.getAppId())
            .stream()
            .filter(env -> deploymentCtx.getEnvIds().contains(env.getUuid()))
            .map(Environment::getEnvironmentType)
            .collect(Collectors.toList());

    return !CollectionUtils.intersection(freezeConfig.getEnvironmentTypes(), envTypesInDeploymentCtx).isEmpty();
  }

  /**
   * @return true if deployment context satisfies the freeze config. That is the freeze should be applied.
   */
  private Boolean appsMatcher(final TimeRangeBasedFreezeConfig freezeConfig, final DeploymentCtx deploymentCtx) {
    boolean freezeEnabledForAllApps = freezeConfig.isFreezeForAllApps();

    if (freezeEnabledForAllApps) {
      return true;
    } else {
      return freezeConfig.getAppIds().contains(deploymentCtx.getAppId());
    }
  }
}
