package io.harness.ngtriggers.beans.source;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("source")
public class NGTriggerSource_1 {
  NGTriggerType type;
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) NGTriggerSpec_1 spec;

  @Builder
  public NGTriggerSource_1(NGTriggerType type, NGTriggerSpec_1 spec) {
    this.type = type;
    this.spec = spec;
  }
}
