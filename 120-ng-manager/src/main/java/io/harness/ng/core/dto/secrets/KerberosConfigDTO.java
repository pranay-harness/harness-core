package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.ng.core.models.BaseSSHSpec;
import io.harness.ng.core.models.KerberosConfig;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Kerberos")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KerberosConfigDTO extends BaseSSHSpecDTO {
  @NotNull private String principal;
  @NotNull private String realm;
  @NotNull private TGTGenerationMethod tgtGenerationMethod;
  private String keyPath;
  @ApiModelProperty(dataType = "string") @SecretReference private SecretRefData password;

  @Override
  public BaseSSHSpec toEntity() {
    return KerberosConfig.builder()
        .principal(getPrincipal())
        .realm(getRealm())
        .keyPath(getKeyPath())
        .password(getPassword())
        .tgtGenerationMethod(getTgtGenerationMethod())
        .build();
  }
}
