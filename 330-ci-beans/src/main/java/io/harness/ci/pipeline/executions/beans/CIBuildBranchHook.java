package io.harness.ci.pipeline.executions.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public class CIBuildBranchHook {
  private String name;
  private String link;
  private String state;
  private List<CIBuildCommit> commits;
  private List<CIBuildCommit> triggerCommits;
}
