package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ng.core.models.BaseSSHSpec;
import io.harness.ng.core.models.KerberosConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Kerberos")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KerberosConfigDTO extends BaseSSHSpecDTO {
  @NotNull private String principal;
  @NotNull private String realm;
  @NotNull private TGTGenerationMethod tgtGenerationMethod;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      property = "tgtGenerationMethod", visible = true)
  @Valid
  private TGTGenerationSpecDTO spec;

  @Override
  public BaseSSHSpec toEntity() {
    return KerberosConfig.builder()
        .principal(getPrincipal())
        .realm(getRealm())
        .tgtGenerationMethod(getTgtGenerationMethod())
        .spec(Optional.ofNullable(getSpec()).map(TGTGenerationSpecDTO::toEntity).orElse(null))
        .build();
  }

  @Builder
  public KerberosConfigDTO(
      String principal, String realm, TGTGenerationMethod tgtGenerationMethod, TGTGenerationSpecDTO spec) {
    this.principal = principal;
    this.realm = realm;
    this.tgtGenerationMethod = tgtGenerationMethod;
    this.spec = spec;
  }
}
