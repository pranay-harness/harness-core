package io.harness.ngtriggers.beans.target;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("target")
public class NGTriggerTarget {
  String targetIdentifier;
  TargetType type;
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) TargetSpec spec;

  @Builder
  public NGTriggerTarget(String targetIdentifier, TargetType type, TargetSpec spec) {
    this.targetIdentifier = targetIdentifier;
    this.type = type;
    this.spec = spec;
  }
}
