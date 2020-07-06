package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceAccountDTO extends KubernetesAuthCredentialDTO {
  @JsonProperty("service-acccount-token") String serviceAccountToken;
}
