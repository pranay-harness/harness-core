/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.entities.embedded.kubernetescluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.K8sOpenIdConnect")
public class K8sOpenIdConnect implements KubernetesAuth {
  String oidcIssuerUrl;
  String oidcClientIdRef;
  String oidcUsername;
  String oidcUsernameRef;
  String oidcPasswordRef;
  String oidcSecretRef;
  String oidcScopes;
}
