package io.harness.delegate.beans.connector.k8Connector;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(KubernetesConfigConstants.SERVICE_ACCOUNT)
public class KubernetesServiceAccountDTO extends KubernetesAuthCredentialDTO {
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData serviceAccountTokenRef;
}
