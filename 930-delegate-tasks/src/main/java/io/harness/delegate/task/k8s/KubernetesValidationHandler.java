/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.k8Connector.K8sValidationParams;
import io.harness.delegate.task.ConnectorValidationHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(CDP)
public class KubernetesValidationHandler implements ConnectorValidationHandler {
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;

  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final K8sValidationParams k8sValidationParams = (K8sValidationParams) connectorValidationParams;
    return k8sTaskHelperBase.validate(
        k8sValidationParams.getKubernetesClusterConfigDTO(), k8sValidationParams.getEncryptedDataDetails());
  }
}
