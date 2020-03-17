package software.wings.delegatetasks.validation.capabilitycheck;

import static software.wings.delegatetasks.validation.PCFCommandValidation.CONNECTION_TIMED_OUT;

import com.google.inject.Inject;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.PcfConfig;
import software.wings.delegatetasks.validation.capabilities.PcfConnectivityCapability;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.service.intfc.security.EncryptionService;

@Slf4j
public class PcfConnectivityCapabilityCheck implements CapabilityCheck {
  @Inject private EncryptionService encryptionService;
  @Inject private PcfDeploymentManager pcfDeploymentManager;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfConnectivityCapability pcfConnectivityCapability = (PcfConnectivityCapability) delegateCapability;
    PcfConfig pcfConfig = pcfConnectivityCapability.getPcfConfig();
    try {
      if (pcfConnectivityCapability.getEncryptionDetails() != null) {
        encryptionService.decrypt(pcfConfig, pcfConnectivityCapability.getEncryptionDetails());
      }
      String validationErrorMsg = pcfDeploymentManager.checkConnectivity(pcfConfig);
      return CapabilityResponse.builder()
          .delegateCapability(pcfConnectivityCapability)
          .validated(!validationErrorMsg.toLowerCase().contains(CONNECTION_TIMED_OUT))
          .build();
    } catch (Exception e) {
      logger.error("Failed to Decrypt pcfConfig, RepoUrl: {}", pcfConfig.getEndpointUrl());
      return CapabilityResponse.builder().delegateCapability(pcfConnectivityCapability).validated(false).build();
    }
  }
}
