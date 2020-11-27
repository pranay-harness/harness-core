package io.harness.ngtriggers.beans.config;

import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.target.NGTriggerTarget;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NGTriggerConfig implements NGTriggerInterface {
  String name;
  @NotNull String identifier;
  String description;
  NGTriggerTarget target;
  NGTriggerSource source;
}
