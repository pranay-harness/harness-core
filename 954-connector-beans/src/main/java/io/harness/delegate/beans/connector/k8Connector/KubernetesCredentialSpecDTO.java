/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
  @JsonSubTypes.Type(value = KubernetesClusterDetailsDTO.class, name = KubernetesConfigConstants.MANUAL_CREDENTIALS)
})
public interface KubernetesCredentialSpecDTO {}
