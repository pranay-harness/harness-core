package io.harness.resourcegroup.remote.dto;

import io.harness.resourcegroup.model.ResourceType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceTypeDTO {
  @ApiModelProperty(required = true) @NotEmpty List<ResourceType> resourceTypes;
}
