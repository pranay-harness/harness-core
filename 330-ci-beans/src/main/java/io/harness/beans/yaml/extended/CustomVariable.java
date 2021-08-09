package io.harness.beans.yaml.extended;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
public interface CustomVariable {
  enum Type {
    @JsonProperty("Text") TEXT,
    @JsonProperty("Secret") SECRET;
  }
  Type getType();
}
