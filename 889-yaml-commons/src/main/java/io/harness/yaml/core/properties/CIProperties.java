package io.harness.yaml.core.properties;

import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.yaml.core.properties.CIProperties")
public class CIProperties {
  CodeBase codebase;
}
