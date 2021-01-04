package io.harness.yaml.extended.ci.codebase;

import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.BRANCH_TYPE;
import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.TAG_TYPE;

import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.TagBuildSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = BranchBuildSpec.class, name = BRANCH_TYPE)
  , @JsonSubTypes.Type(value = TagBuildSpec.class, name = TAG_TYPE)
})
public interface BuildSpec {}