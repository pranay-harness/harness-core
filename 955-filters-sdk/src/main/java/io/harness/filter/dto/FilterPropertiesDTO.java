package io.harness.filter.dto;

import io.harness.filter.FilterType;
import io.harness.filter.entity.FilterProperties;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.annotations.ApiModel;
import java.util.Map;
import lombok.Data;

@Data
@ApiModel("FilterProperties")
@JsonTypeInfo(use = Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "filterType")
public abstract class FilterPropertiesDTO {
  Map<String, String> tags;
  FilterType filterType;

  public abstract FilterProperties toEntity();
}
