package io.harness.resourcegroup.remote.dto;

import io.harness.resourcegroup.beans.ValidatorType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ResourceType", description = "Contains list of Resource Type")
public class ResourceTypeDTO {
  @ApiModelProperty(required = true) @NotEmpty List<ResourceType> resourceTypes;

  @Value
  @Builder
  public static class ResourceType {
    String name;
    List<ValidatorType> validatorTypes;
  }
}
