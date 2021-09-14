/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class K8sTaskCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public static List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ConnectorConfigDTO connectorConfigDTO, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    KubernetesClusterConfigDTO kubernetesClusterConfigDTO = (KubernetesClusterConfigDTO) connectorConfigDTO;
    KubernetesCredentialDTO credential = kubernetesClusterConfigDTO.getCredential();
    if (credential.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO k8sManualCreds = (KubernetesClusterDetailsDTO) credential.getConfig();
      capabilityList.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          k8sManualCreds.getMasterUrl(), maskingEvaluator));
    } else if (credential.getKubernetesCredentialType() != KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      throw new UnknownEnumTypeException(
          "Kubernetes Credential Type", String.valueOf(credential.getKubernetesCredentialType()));
    }
    populateDelegateSelectorCapability(capabilityList, kubernetesClusterConfigDTO.getDelegateSelectors());
    return capabilityList;
  }
}
