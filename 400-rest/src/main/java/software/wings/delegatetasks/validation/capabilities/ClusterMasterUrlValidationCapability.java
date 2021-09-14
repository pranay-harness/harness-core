/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.validation.capabilities;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.settings.SettingValue;

import java.time.Duration;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class ClusterMasterUrlValidationCapability implements ExecutionCapability {
  @NotNull ContainerServiceParams containerServiceParams;
  CapabilityType capabilityType = CapabilityType.CLUSTER_MASTER_URL;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) value;
      return kubernetesClusterConfig.getMasterUrl();
    } else if (value instanceof GcpConfig) {
      return "GCP:" + containerServiceParams.getClusterName();
    } else if (value instanceof AzureConfig) {
      String subscriptionId = containerServiceParams.getSubscriptionId();
      String resourceGroup = containerServiceParams.getResourceGroup();
      return "Azure:" + subscriptionId + resourceGroup + containerServiceParams.getClusterName();
    } else {
      throw new InvalidRequestException("No capability Basis Supported for : " + value.getSettingType());
    }
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
