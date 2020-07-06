package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesDelegateDetailsDTO implements KubernetesCredentialDTO {
  @JsonProperty("inheritConfigFromDelegate") String delegateName;
}
