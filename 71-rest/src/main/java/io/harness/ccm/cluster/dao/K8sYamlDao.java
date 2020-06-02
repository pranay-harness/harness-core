package io.harness.ccm.cluster.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.ccm.cluster.entities.K8sYaml;
import io.harness.ccm.cluster.entities.K8sYaml.K8sYamlKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class K8sYamlDao {
  private final HPersistence hPersistence;

  @Inject
  public K8sYamlDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public String ensureYamlSaved(String accountId, String clusterId, String uid, String resourceVersion, String yaml) {
    K8sYaml k8sYaml = K8sYaml.builder()
                          .accountId(accountId)
                          .clusterId(clusterId)
                          .uid(uid)
                          .resourceVersion(resourceVersion)
                          .yaml(yaml)
                          .build();
    saveIfNotPresent(k8sYaml);
    return k8sYaml.getHash();
  }

  public K8sYaml getYaml(String accountId, String hash) {
    return hPersistence.createQuery(K8sYaml.class)
        .field(K8sYamlKeys.accountId)
        .equal(accountId)
        .field(K8sYamlKeys.hash)
        .equal(hash)
        .get();
  }

  void saveIfNotPresent(K8sYaml yamlRecord) {
    try {
      hPersistence.save(yamlRecord);
    } catch (DuplicateKeyException e) {
      logger.debug("Ignoring exception for yaml already present", e);
    }
  }
}
