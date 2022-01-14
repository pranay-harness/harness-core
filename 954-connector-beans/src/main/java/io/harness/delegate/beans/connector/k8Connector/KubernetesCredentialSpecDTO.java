package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSubTypes({
  @JsonSubTypes.Type(value = KubernetesClusterDetailsDTO.class, name = KubernetesConfigConstants.MANUAL_CREDENTIALS)
})
@Schema(name = "KubernetesCredentialSpec", description = "This contains kubernetes credentials spec details")
public interface KubernetesCredentialSpecDTO {}
