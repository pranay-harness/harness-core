package io.harness.ng.core.models;

import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.TGTGenerationSpecDTO;
import io.harness.ng.core.dto.secrets.TGTPasswordSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Password")
public class TGTPasswordSpec extends TGTGenerationSpec {
  private SecretRefData password;

  @Override
  public TGTGenerationSpecDTO toDTO() {
    return TGTPasswordSpecDTO.builder().password(getPassword()).build();
  }
}
