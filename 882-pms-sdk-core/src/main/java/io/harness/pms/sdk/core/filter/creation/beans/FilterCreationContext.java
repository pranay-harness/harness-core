package io.harness.pms.sdk.core.filter.creation.beans;

import io.harness.pms.yaml.YamlField;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterCreationContext {
  YamlField currentField;
}
