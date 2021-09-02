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
public class CIBuildPRHook {
  private Long id;
  private String link;
  private String title;
  private String body;
  private String sourceRepo;
  private String sourceBranch;
  private String targetBranch;
  private String state;
  private List<CIBuildCommit> commits;
  private List<CIBuildCommit> triggerCommits;
}
