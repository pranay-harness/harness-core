package io.harness.cdng.k8s;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InstanceSelectionWrapper {
  K8sInstanceUnitType type;
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) InstanceSelectionBase spec;

  @Builder
  public InstanceSelectionWrapper(K8sInstanceUnitType type, InstanceSelectionBase spec) {
    this.type = type;
    this.spec = spec;
  }
}
