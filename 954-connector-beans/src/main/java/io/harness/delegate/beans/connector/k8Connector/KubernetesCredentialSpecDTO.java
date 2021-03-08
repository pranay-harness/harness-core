package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
  @JsonSubTypes.Type(value = KubernetesClusterDetailsDTO.class, name = KubernetesConfigConstants.MANUAL_CREDENTIALS)
  ,
      @JsonSubTypes.Type(
          value = KubernetesDelegateDetailsDTO.class, name = KubernetesConfigConstants.INHERIT_FROM_DELEGATE)
})
public interface KubernetesCredentialSpecDTO {}
