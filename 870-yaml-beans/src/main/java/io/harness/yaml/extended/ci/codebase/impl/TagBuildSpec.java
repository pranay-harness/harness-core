package io.harness.yaml.extended.ci.codebase.impl;

import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.TAG_TYPE;

import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.codebase.BuildSpec;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.yaml.extended.ci.impl.BranchBuildSpec")
@JsonTypeName(TAG_TYPE)
public class TagBuildSpec implements BuildSpec {
  @NotNull ParameterField<String> tag;
}
