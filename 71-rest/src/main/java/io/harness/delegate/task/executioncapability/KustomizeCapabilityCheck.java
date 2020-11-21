package io.harness.delegate.task.executioncapability;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;

import software.wings.delegatetasks.validation.K8sValidationHelper;

import com.google.inject.Inject;

public class KustomizeCapabilityCheck implements CapabilityCheck {
  @Inject private K8sValidationHelper validationHelper;
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    KustomizeCapability capability = (KustomizeCapability) delegateCapability;
    return CapabilityResponse.builder()
        .validated(validationHelper.doesKustomizePluginDirExist(capability.getKustomizeConfig()))
        .delegateCapability(capability)
        .build();
  }
}
