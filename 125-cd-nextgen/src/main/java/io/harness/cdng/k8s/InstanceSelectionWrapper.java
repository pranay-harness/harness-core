package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@RecasterAlias("io.harness.cdng.k8s.InstanceSelectionWrapper")
public class InstanceSelectionWrapper {
  K8sInstanceUnitType type;
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) InstanceSelectionBase spec;

  @Builder
  public InstanceSelectionWrapper(K8sInstanceUnitType type, InstanceSelectionBase spec) {
    this.type = type;
    this.spec = spec;
  }
}
