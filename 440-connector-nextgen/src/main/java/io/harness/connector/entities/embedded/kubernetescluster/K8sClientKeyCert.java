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
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.K8sClientKeyCert")
public class K8sClientKeyCert implements KubernetesAuth {
  String caCertRef;
  String clientCertRef;
  String clientKeyRef;
  String clientKeyPassphraseRef;
  String clientKeyAlgo;
}
