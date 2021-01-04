package io.harness.ng.core.dto.secrets;

import static io.harness.yamlSchema.NGSecretReferenceConstants.SECRET_REF_PATTERN;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.ng.core.models.TGTGenerationSpec;
import io.harness.ng.core.models.TGTPasswordSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Password")
public class TGTPasswordSpecDTO extends TGTGenerationSpecDTO implements DecryptableEntity {
  @ApiModelProperty(dataType = "string")
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  private SecretRefData password;

  @Override
  public TGTGenerationSpec toEntity() {
    return TGTPasswordSpec.builder().password(getPassword()).build();
  }
}
