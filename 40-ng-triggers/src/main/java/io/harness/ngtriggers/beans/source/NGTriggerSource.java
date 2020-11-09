package io.harness.ngtriggers.beans.source;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

@Data
@NoArgsConstructor
@JsonTypeName("source")
public class NGTriggerSource {
  NGTriggerType type;
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) NGTriggerSpec spec;

  @Builder
  public NGTriggerSource(NGTriggerType type, NGTriggerSpec spec) {
    this.type = type;
    this.spec = spec;
  }
}
