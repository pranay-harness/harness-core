package io.harness.ccm.cluster;

import com.google.inject.Inject;

import io.harness.ccm.cluster.dao.K8sYamlDao;
import io.harness.ccm.cluster.entities.K8sYaml;

public class K8sYamlServiceImpl implements K8sYamlService {
  @Inject K8sYamlDao k8sYamlDao;

  @Override
  public K8sYaml get(String accountId, String uuid) {
    return k8sYamlDao.getYaml(accountId, uuid);
  }
}
