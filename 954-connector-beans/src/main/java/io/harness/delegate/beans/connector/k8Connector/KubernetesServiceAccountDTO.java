package io.harness.delegate.beans.connector.k8Connector;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(KubernetesConfigConstants.SERVICE_ACCOUNT)
@Schema(name = "KubernetesServiceAccount", description = "This contains kubernetes service account details")
public class KubernetesServiceAccountDTO extends KubernetesAuthCredentialDTO {
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData serviceAccountTokenRef;
}
