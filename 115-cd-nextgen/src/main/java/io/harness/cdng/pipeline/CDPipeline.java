package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.yaml.core.Tag;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.Pipeline;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPipeline implements Pipeline {
  @EntityName String name;
  @EntityIdentifier String identifier;
  String description;
  List<Tag> tags;
  @Singular List<StageElementWrapper> stages;
}
