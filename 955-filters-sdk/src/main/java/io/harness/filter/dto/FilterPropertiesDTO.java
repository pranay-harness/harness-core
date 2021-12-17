package io.harness.filter.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Data;

@Data
@ApiModel("FilterProperties")
@JsonTypeInfo(use = Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "filterType")
@OwnedBy(DX)
@Schema(name = "FilterProperties", description = "Properties of the Filter entity defined in Harness")
public abstract class FilterPropertiesDTO {
  @Schema(description = "Tags of the Filter in key-value pair.") Map<String, String> tags;
  @Schema(description = "This indicates the filter is of which Entity. Example Connector, Pipeline etc.")
  FilterType filterType;
}
