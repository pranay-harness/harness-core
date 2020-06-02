package io.harness.cdng.pipeline;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.Tag;
import io.harness.yaml.core.auxiliary.intfc.StageWrapper;
import io.harness.yaml.core.intfc.Pipeline;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonTypeName("pipeline")
public class CDPipeline implements Pipeline {
  private String name;
  private String description;
  private List<Tag> tags;
  @Singular private List<StageWrapper> stages;
  private String identifier;
}
