package io.harness.resourcegroup.remote.dto;

import io.harness.resourcegroup.model.ResourceSelector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceGroupDTO {
  @ApiModelProperty(required = true) @NotEmpty String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @ApiModelProperty(required = true) @NotEmpty String identifier;
  @ApiModelProperty(required = true) @NotEmpty String name;
  @ApiModelProperty(required = true) @NotNull Boolean system;
  @ApiModelProperty(required = true) @NotEmpty @Valid List<ResourceSelector> resourceSelectors;
  String description;
}
