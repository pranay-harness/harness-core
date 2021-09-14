/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.cloudprovider.gke;

import io.harness.k8s.model.KubernetesConfig;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;

import java.util.List;
import java.util.Map;

/**
 * Created by bzane on 2/21/17.
 */
public interface GkeClusterService {
  /**
   * Creates a new cluster unless a cluster with the given name already exists
   */
  KubernetesConfig createCluster(SettingAttribute computeProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String zoneClusterName, String namespace,
      Map<String, String> params);

  /**
   * Gets the details about a cluster
   */
  KubernetesConfig getCluster(SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      String zoneClusterName, String namespace, boolean isInstanceSync);

  KubernetesConfig getCluster(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String locationClusterName, String namespace, boolean isInstanceSync);

  /**
   * Lists the available clusters
   */
  List<String> listClusters(SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails);
}
