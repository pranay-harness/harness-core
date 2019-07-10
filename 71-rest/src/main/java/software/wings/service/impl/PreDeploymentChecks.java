package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.FEATURE_UNAVAILABLE;
import static io.harness.exception.WingsException.USER;
import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.licensing.LicenseService;
import software.wings.service.impl.deployment.checks.AccountExpirationChecker;
import software.wings.service.impl.deployment.checks.DeploymentFreezeChecker;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.service.intfc.deployment.PreDeploymentChecker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class PreDeploymentChecks {
  private AppService appService;
  private LicenseService licenseService;
  private GovernanceConfigService governanceConfigService;
  private AccountService accountService;
  private WorkflowPreDeploymentValidator workflowPreDeploymentValidator;
  private PipelinePreDeploymentValidator pipelinePreDeploymentValidator;

  enum CheckType { ACCOUNT_EXPIRY, DEPLOYMENT_FREEZE }

  private Map<CheckType, PreDeploymentChecker> checks = new HashMap<>();

  @Inject
  public PreDeploymentChecks(AppService appService, LicenseService licenseService,
      GovernanceConfigService governanceConfigService, AccountService accountService,
      WorkflowPreDeploymentValidator workflowPreDeploymentValidator,
      PipelinePreDeploymentValidator pipelinePreDeploymentValidator) {
    this.appService = appService;
    this.licenseService = licenseService;
    this.governanceConfigService = governanceConfigService;
    this.accountService = accountService;
    this.workflowPreDeploymentValidator = workflowPreDeploymentValidator;
    this.pipelinePreDeploymentValidator = pipelinePreDeploymentValidator;

    populateChecks();
  }

  private void populateChecks() {
    PreDeploymentChecker noOpChecker = (accountId, appId) -> {};
    checks.put(CheckType.ACCOUNT_EXPIRY, new AccountExpirationChecker(licenseService));
    checks.put(CheckType.DEPLOYMENT_FREEZE, new DeploymentFreezeChecker(governanceConfigService));

    for (CheckType checkType : CheckType.values()) {
      if (!checks.containsKey(checkType)) {
        logger.error(
            "You defined a value in CheckType enum but did not assign it a value in map. Key: {} . A No-op value will be added to prevent NPE.",
            checkType);
        checks.put(checkType, noOpChecker);
      }
    }
  }

  public void isDeploymentAllowed(String appId) throws WingsException {
    Application application = appService.get(appId, false);
    requireNonNull(application, "Application not found. Is the application ID correct? AppId: " + appId);
    String accountId = application.getAccountId();

    checks.get(CheckType.ACCOUNT_EXPIRY).check(accountId, appId);
    checks.get(CheckType.DEPLOYMENT_FREEZE).check(accountId, appId);
  }

  public void checkIfWorkflowUsingRestrictedFeatures(@NotNull Workflow workflow) {
    String accountType = accountService.getAccountType(workflow.getAccountId()).orElse(AccountType.PAID);
    List<ValidationError> validationErrorList = workflowPreDeploymentValidator.validate(accountType, workflow);
    if (isNotEmpty(validationErrorList)) {
      String validationMessage = validationErrorList.get(0).getMessage();
      logger.warn("Pre-deployment restricted features check failed for workflowId ={} with reason={} ",
          workflow.getUuid(), validationMessage);
      throw new WingsException(FEATURE_UNAVAILABLE, validationMessage, USER).addParam("message", validationMessage);
    }
  }

  public void checkIfPipelineUsingRestrictedFeatures(@NotNull Pipeline pipeline) {
    String accountType = accountService.getAccountType(pipeline.getAccountId()).orElse(AccountType.PAID);
    List<ValidationError> validationErrorList = pipelinePreDeploymentValidator.validate(accountType, pipeline);
    if (isNotEmpty(validationErrorList)) {
      String validationMessage = validationErrorList.get(0).getMessage();
      logger.warn("Pre-deployment restricted features check failed for pipelinedId ={} with reason={} ",
          pipeline.getUuid(), validationMessage);
      throw new WingsException(FEATURE_UNAVAILABLE, validationMessage, WingsException.USER);
    }
  }
}
