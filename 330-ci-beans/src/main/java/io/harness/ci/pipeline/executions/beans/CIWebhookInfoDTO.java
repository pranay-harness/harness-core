package io.harness.ci.pipeline.executions.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.beans.build.PublishedArtifact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public class CIWebhookInfoDTO {
  private String event;
  private CIBuildAuthor author;
  private CIBuildBranchHook branch;
  private CIBuildPRHook pullRequest;
  private List<PublishedArtifact> publishedArtifacts;
}
