package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
  @JsonSubTypes.Type(value = KubernetesClusterDetailsDTO.class, name = KubernetesConfigConstants.MANUAL_CREDENTIALS)
})
public interface KubernetesCredentialSpecDTO {}
