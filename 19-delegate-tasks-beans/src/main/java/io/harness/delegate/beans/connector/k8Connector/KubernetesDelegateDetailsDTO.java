package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonTypeName(KubernetesConfigConstants.INHERIT_FROM_DELEGATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesDelegateDetailsDTO implements KubernetesCredentialSpecDTO {
  @NotNull String delegateName;
}
