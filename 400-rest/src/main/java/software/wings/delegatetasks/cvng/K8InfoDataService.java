/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.cvng;

import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

import java.util.List;

public interface K8InfoDataService {
  @DelegateTaskType(TaskType.K8_FETCH_NAMESPACES)
  List<String> getNameSpaces(
      DataCollectionConnectorBundle bundle, List<EncryptedDataDetail> encryptedDataDetails, String filter);

  @DelegateTaskType(TaskType.K8_FETCH_WORKLOADS)
  List<String> getWorkloads(String namespace, DataCollectionConnectorBundle bundle,
      List<EncryptedDataDetail> encryptedDataDetails, String filter);

  @DelegateTaskType(TaskType.K8_FETCH_EVENTS)
  List<String> checkCapabilityToGetEvents(
      DataCollectionConnectorBundle bundle, List<EncryptedDataDetail> encryptedDataDetails);

  KubernetesConfig getDecryptedKubernetesConfig(
      KubernetesClusterConfigDTO kubernetesClusterConfig, List<EncryptedDataDetail> encryptedDataDetails);
}
