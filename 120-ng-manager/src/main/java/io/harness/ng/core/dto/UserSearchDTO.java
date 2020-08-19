package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSearchDTO {
  @ApiModelProperty(required = true) @NotEmpty String name;
  @ApiModelProperty(required = true) @NotEmpty String email;
  @ApiModelProperty(required = true) @NotEmpty String uuid;
}