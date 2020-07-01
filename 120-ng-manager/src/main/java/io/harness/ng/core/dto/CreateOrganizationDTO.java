package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrganizationDTO {
  @ApiModelProperty(required = true) @NotEmpty @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NotEmpty @EntityName String name;
  @ApiModelProperty(required = true) @NotEmpty String color;
  String description;
  @ApiModelProperty(required = true) @NotNull List<String> tags;
}
