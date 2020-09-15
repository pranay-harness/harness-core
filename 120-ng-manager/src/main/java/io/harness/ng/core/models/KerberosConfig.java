package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.BaseSSHSpecDTO;
import io.harness.ng.core.dto.secrets.KerberosConfigDTO;
import io.harness.ng.core.dto.secrets.TGTGenerationMethod;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Kerberos")
public class KerberosConfig extends BaseSSHSpec {
  private String principal;
  private String realm;
  private TGTGenerationMethod tgtGenerationMethod;
  private String keyPath;
  private SecretRefData password;

  @Override
  public BaseSSHSpecDTO toDTO() {
    return KerberosConfigDTO.builder()
        .principal(getPrincipal())
        .realm(getRealm())
        .keyPath(getKeyPath())
        .tgtGenerationMethod(getTgtGenerationMethod())
        .password(getPassword())
        .build();
  }
}
