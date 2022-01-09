/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.BaseSSHSpec;
import io.harness.ng.core.models.KerberosConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Kerberos")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Schema(name = "KerberosConfig", description = "This is the Kerberos configuration details, defined in Harness.")
public class KerberosConfigDTO extends BaseSSHSpecDTO {
  @Schema(description = "This is the authorization role, the user/service has in the realm.")
  @NotNull
  private String principal;
  @Schema(description = "Name of the Realm.") @NotNull private String realm;
  @JsonTypeId private TGTGenerationMethod tgtGenerationMethod;

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
