package io.harness.ng.core.service.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceRequestDTO {
  @ApiModelProperty(required = true) @NotEmpty @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NotEmpty String orgIdentifier;
  @ApiModelProperty(required = true) @NotEmpty String projectIdentifier;

  @EntityName String name;
  String description;
}
