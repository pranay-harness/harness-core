package io.harness.yaml.core.properties;

import io.harness.annotation.RecasterAlias;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.yaml.core.properties.NGProperties")
@RecasterAlias("io.harness.yaml.core.properties.NGProperties")
public class NGProperties {
  @ApiModelProperty(dataType = "io.harness.yaml.core.properties.CIProperties") JsonNode ci;
}
