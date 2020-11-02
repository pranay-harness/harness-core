package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.pcf.model.PcfConstants.PCF_CONNECTIVITY_SUCCESS;

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
        encryptionService.decrypt(pcfConfig, pcfConnectivityCapability.getEncryptionDetails(), false);
      }
      String validationErrorMsg =
          pcfDeploymentManager.checkConnectivity(pcfConfig, pcfConnectivityCapability.isLimitPcfThreads());
      return CapabilityResponse.builder()
          .delegateCapability(pcfConnectivityCapability)
          .validated(PCF_CONNECTIVITY_SUCCESS.equals(validationErrorMsg))
          .build();
    } catch (Exception e) {
      log.error("Failed to Decrypt pcfConfig, RepoUrl: {}", pcfConfig.getEndpointUrl());
      return CapabilityResponse.builder().delegateCapability(pcfConnectivityCapability).validated(false).build();
    }
  }
}
