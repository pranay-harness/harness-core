package io.harness.cvng.servicelevelobjective.beans;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserJourneyDTO implements YamlDTO {
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String orgIdentifier;
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String projectIdentifier;
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NotNull @NGEntityName String name;
}
