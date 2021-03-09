package io.harness.ccm.cluster.entities;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.settings.SettingValue;

import java.util.List;

public interface KubernetesCluster {
  K8sClusterConfig toK8sClusterConfig(SettingValue cloudProvider, List<EncryptedDataDetail> encryptionDetails);
}
