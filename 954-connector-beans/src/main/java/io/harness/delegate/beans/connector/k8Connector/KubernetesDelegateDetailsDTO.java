package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(KubernetesConfigConstants.INHERIT_FROM_DELEGATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "KubernetesDelegateDetails", description = "This contains kubernetes connector delegate details")
public class KubernetesDelegateDetailsDTO implements KubernetesCredentialSpecDTO {
  //  @NotNull @Size(min = 1) Set<String> delegateSelectors;
  // delete this
}
