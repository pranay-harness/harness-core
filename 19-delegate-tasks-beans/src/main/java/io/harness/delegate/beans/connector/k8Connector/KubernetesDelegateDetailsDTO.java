package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("InheritFromDelegate")
public class KubernetesDelegateDetailsDTO implements KubernetesCredentialDTO {
  @JsonProperty("inheritConfigFromDelegate") @NotBlank String delegateName;
}
