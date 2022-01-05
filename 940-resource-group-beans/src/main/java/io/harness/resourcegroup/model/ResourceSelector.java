package io.harness.resourcegroup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = StaticResourceSelector.class, name = "StaticResourceSelector")
  , @JsonSubTypes.Type(value = DynamicResourceSelector.class, name = "DynamicResourceSelector"),
      @JsonSubTypes.Type(value = ResourceSelectorByScope.class, name = "ResourceSelectorByScope")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ResourceSelector {}
