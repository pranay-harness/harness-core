package io.harness.resourcegroup.remote.dto;

import io.harness.resourcegroup.model.ResourceSelector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Size;
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
  @ApiModelProperty(required = true) @Size(max = 128) @NotEmpty String identifier;
  @ApiModelProperty(required = true) @Size(max = 128) @NotEmpty String name;
  @Size(max = 256) @Valid List<ResourceSelector> resourceSelectors;
  boolean fullScopeSelected;
  @Size(max = 128) Map<String, String> tags;
  @Size(max = 1024) String description;
  String color;
}
