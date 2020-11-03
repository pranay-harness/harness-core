package software.wings.delegatetasks.validation;

import static io.harness.pcf.model.PcfConstants.CF_APP_AUTOSCALAR_VALIDATION;
import static io.harness.pcf.model.PcfConstants.CF_CLI_NEED_TO_BE_INSTALLED;
import static java.util.Collections.singletonList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.task.executioncapability.ProcessExecutorCapabilityCheck;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.PcfConfig;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandTaskParameters;
import software.wings.helpers.ext.pcf.request.PcfRunPluginCommandRequest;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class PCFCommandValidation extends AbstractDelegateValidateTask {
  public static final String CONNECTION_TIMED_OUT = "connection timed out";

  @Inject private transient PcfDeploymentManager pcfDeploymentManager;
  @Inject private transient EncryptionService encryptionService;

  public PCFCommandValidation(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DelegateConnectionResult> validate() {
    boolean validated = false;
    PcfCommandRequest commandRequest;
    final List<EncryptedDataDetail> encryptionDetails;
    if (getParameters()[0] instanceof PcfCommandTaskParameters) {
      PcfCommandTaskParameters pcfCommandTaskParameters = (PcfCommandTaskParameters) getParameters()[0];
      commandRequest = pcfCommandTaskParameters.getPcfCommandRequest();
      encryptionDetails = pcfCommandTaskParameters.getEncryptedDataDetails();
    } else {
      commandRequest = (PcfCommandRequest) getParameters()[0];
      encryptionDetails = getEncryptedDataDetails(commandRequest);
    }

    PcfConfig pcfConfig = commandRequest.getPcfConfig();
    log.info("Running validation for task {} ", delegateTaskId);

    try {
      if (encryptionDetails != null) {
        encryptionService.decrypt(pcfConfig, encryptionDetails, false);
      }

      String validationErrorMsg = pcfDeploymentManager.checkConnectivity(
          pcfConfig, commandRequest.isLimitPcfThreads(), commandRequest.isIgnorePcfConnectionContextCache());
      validated = !validationErrorMsg.toLowerCase().contains(CONNECTION_TIMED_OUT);
      if (!validated) {
        printWarning("Failed to verify PCF Connectivity");
      }
    } catch (Exception e) {
      String errorMsg = new StringBuilder(64)
                            .append("Failed to Decrypt pcfConfig, ")
                            .append("RepoUrl: ")
                            .append(pcfConfig.getEndpointUrl())
                            .toString();
      log.error(errorMsg);
    }

    if (validated && pcfCliValidationRequired(commandRequest)) {
      // Here we are using new DelegateCapability Framework code. But eventually, this validation
      // should become part of this framework and this class should be deprecated and removed later
      ProcessExecutorCapabilityCheck executorCapabilityCheck = new ProcessExecutorCapabilityCheck();
      CapabilityResponse response = executorCapabilityCheck.performCapabilityCheck(
          ProcessExecutorCapability.builder()
              .capabilityType(CapabilityType.PROCESS_EXECUTOR)
              .category("PCF")
              .processExecutorArguments(Arrays.asList("/bin/sh", "-c", "cf --version"))
              .build());

      validated = response.isValidated();
      if (!validated) {
        printWarning("CF CLI check failed. Could not find CF CLI installed");
      }
    }

    if (validated && needToCheckAppAutoscalarPluginInstall(commandRequest)) {
      try {
        validated = pcfDeploymentManager.checkIfAppAutoscalarInstalled();
        if (!validated) {
          printWarning(new StringBuilder("Could not find App Autoscalar plugin installed. ")
                           .append("CF PLUGIN HOME Used: ")
                           .append(pcfDeploymentManager.resolvePcfPluginHome())
                           .toString());
        }
      } catch (Exception e) {
        log.error("Failed to Validate App-Autoscalar Plugin installed");
        validated = false;
      }
    }
    if (!validated) {
      log.warn("This delegate failed to verify Pivotal Task Execution");
    }

    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  private void printWarning(String message) {
    log.warn(message);
  }

  @VisibleForTesting
  boolean needToCheckAppAutoscalarPluginInstall(PcfCommandRequest commandRequest) {
    boolean checkAppAutoscalarPluginInstall = false;
    if (commandRequest instanceof PcfCommandSetupRequest || commandRequest instanceof PcfCommandDeployRequest
        || commandRequest instanceof PcfCommandRollbackRequest
        || commandRequest instanceof PcfCommandRouteUpdateRequest) {
      checkAppAutoscalarPluginInstall = commandRequest.isUseAppAutoscalar();
    }

    return checkAppAutoscalarPluginInstall;
  }

  @Override
  public List<String> getCriteria() {
    if (getParameters()[0] instanceof PcfCommandTaskParameters) {
      PcfCommandTaskParameters commandTaskParameters = (PcfCommandTaskParameters) getParameters()[0];
      List<EncryptedDataDetail> encryptionDetails = commandTaskParameters.getEncryptedDataDetails();
      return singletonList(getCriteria(commandTaskParameters.getPcfCommandRequest(), encryptionDetails));
    }

    PcfCommandRequest pcfCommandRequest = (PcfCommandRequest) getParameters()[0];
    List<EncryptedDataDetail> encryptionDetails = getEncryptedDataDetails(pcfCommandRequest);
    return singletonList(getCriteria(pcfCommandRequest, encryptionDetails));
  }

  @VisibleForTesting
  String getCriteria(PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptionDetails) {
    PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();

    StringBuilder criteria = new StringBuilder(256);
    if (pcfCliValidationRequired(pcfCommandRequest)) {
      criteria.append(CF_CLI_NEED_TO_BE_INSTALLED).append('_');
    }

    if (needToCheckAppAutoscalarPluginInstall(pcfCommandRequest)) {
      criteria.append(CF_APP_AUTOSCALAR_VALIDATION).append('_');
    }

    criteria.append("Pcf:").append(pcfConfig.getEndpointUrl());

    return criteria.toString();
  }

  private List<EncryptedDataDetail> getEncryptedDataDetails(PcfCommandRequest pcfCommandRequest) {
    if (pcfCommandRequest instanceof PcfRunPluginCommandRequest) {
      return ((PcfRunPluginCommandRequest) pcfCommandRequest).getEncryptedDataDetails();
    }
    final Object[] parameters = getParameters();
    if (parameters.length > 1) {
      return (List<EncryptedDataDetail>) parameters[1];
    }
    return null;
  }

  boolean pcfCliValidationRequired(PcfCommandRequest pcfCommandRequest) {
    return pcfCommandRequest instanceof PcfRunPluginCommandRequest || pcfCommandRequest.isUseCfCLI()
        || needToCheckAppAutoscalarPluginInstall(pcfCommandRequest);
  }
}