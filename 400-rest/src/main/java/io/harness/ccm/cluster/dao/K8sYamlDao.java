/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.cluster.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.commons.entities.k8s.K8sYaml;
import io.harness.ccm.commons.entities.k8s.K8sYaml.K8sYamlKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
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
      log.debug("Ignoring exception for yaml already present", e);
    }
  }
}
