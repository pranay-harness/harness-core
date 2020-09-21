package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.ng.core.models.TGTGenerationSpec;
import io.harness.ng.core.models.TGTPasswordSpec;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Password")
public class TGTPasswordSpecDTO extends TGTGenerationSpecDTO {
  @ApiModelProperty(dataType = "string") @SecretReference private SecretRefData password;

  @Override
  public TGTGenerationSpec toEntity() {
    return TGTPasswordSpec.builder().password(getPassword()).build();
  }
}
