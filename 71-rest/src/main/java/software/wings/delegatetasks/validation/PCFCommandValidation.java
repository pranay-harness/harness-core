package software.wings.delegatetasks.validation;

import static io.harness.pcf.model.PcfConstants.CF_APP_AUTOSCALAR_VALIDATION;
import static java.util.Collections.singletonList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
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
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DelegateConnectionResult> validate() {
    boolean validated = false;
    PcfCommandRequest commandRequest = (PcfCommandRequest) getParameters()[0];
    PcfConfig pcfConfig = commandRequest.getPcfConfig();
    final List<EncryptedDataDetail> encryptionDetails = getEncryptedDataDetails(commandRequest);
    logger.info("Running validation for task {} ", delegateTaskId);

    try {
      if (encryptionDetails != null) {
        encryptionService.decrypt(pcfConfig, encryptionDetails);
      }

      String validationErrorMsg = pcfDeploymentManager.checkConnectivity(pcfConfig);
      validated = !validationErrorMsg.toLowerCase().contains(CONNECTION_TIMED_OUT);
    } catch (Exception e) {
      String errorMsg = new StringBuilder(64)
                            .append("Failed to Decrypt pcfConfig, ")
                            .append("RepoUrl: ")
                            .append(pcfConfig.getEndpointUrl())
                            .toString();
      logger.error(errorMsg);
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
    }

    if (validated && needToCheckAppAutoscalarPluginInstall(commandRequest)) {
      try {
        validated = pcfDeploymentManager.checkIfAppAutoscalarInstalled();
      } catch (Exception e) {
        logger.error("Failed to Validate App-Autoscalar Plugin installed");
        validated = false;
      }
    }
    if (!validated) {
      logger.warn("This delegate failed to verify Pivotal connectivity");
    }

    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
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
    return singletonList(getCriteria((PcfCommandRequest) getParameters()[0]));
  }

  @VisibleForTesting
  String getCriteria(PcfCommandRequest pcfCommandRequest) {
    PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
    String criteria = new StringBuilder()
                          .append("Pcf:")
                          .append(pcfConfig.getEndpointUrl())
                          .append("/")
                          .append(pcfConfig.getUsername())
                          .toString();

    if (pcfCliValidationRequired(pcfCommandRequest)) {
      criteria = new StringBuilder(128).append(criteria).append('_').append("cf_cli").toString();
    }

    if (needToCheckAppAutoscalarPluginInstall(pcfCommandRequest)) {
      criteria = new StringBuilder(128).append(criteria).append('_').append(CF_APP_AUTOSCALAR_VALIDATION).toString();
    }

    return criteria;
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