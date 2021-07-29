package io.harness.yaml.extended.ci.codebase;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.BRANCH;
import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.COMMIT_SHA;
import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.PR;
import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.TAG;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.CommitShaBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.PRBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.TagBuildSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = BranchBuildSpec.class, name = BRANCH)
  , @JsonSubTypes.Type(value = TagBuildSpec.class, name = TAG),
      @JsonSubTypes.Type(value = PRBuildSpec.class, name = PR),
      @JsonSubTypes.Type(value = CommitShaBuildSpec.class, name = COMMIT_SHA)
})
@OwnedBy(CI)
public interface BuildSpec {}