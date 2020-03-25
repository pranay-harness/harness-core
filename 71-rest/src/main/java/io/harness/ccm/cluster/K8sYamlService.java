package io.harness.ccm.cluster;

import io.harness.ccm.cluster.entities.K8sYaml;

public interface K8sYamlService { K8sYaml get(String accountId, String uuid); }
