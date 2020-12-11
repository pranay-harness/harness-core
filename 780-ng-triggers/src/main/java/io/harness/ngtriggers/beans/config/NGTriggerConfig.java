package io.harness.ngtriggers.beans.config;

import io.harness.ng.core.common.beans.NGTag;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.target.NGTriggerTarget;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class NGTriggerConfig implements NGTriggerInterface {
  String name;
  @NotNull String identifier;
  String description;
  NGTriggerTarget target;
  NGTriggerSource source;
  Map<String, String> tags;
  @Builder.Default Boolean enabled = Boolean.TRUE;
}
