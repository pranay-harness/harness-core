package io.harness.pms.sdk.core.filter.creation.beans;

import io.harness.pms.yaml.YamlField;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterCreationContext {
  YamlField currentField;
}
