package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonTypeName("ServiceAccount")
public class KubernetesServiceAccountDTO extends KubernetesAuthCredentialDTO {
  @NotNull @SecretReference SecretRefData serviceAccountTokenRef;
}
