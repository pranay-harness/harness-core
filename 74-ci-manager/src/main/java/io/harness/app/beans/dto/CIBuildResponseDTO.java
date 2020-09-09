package io.harness.app.beans.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.app.beans.entities.CIBuildAuthor;
import io.harness.app.beans.entities.CIBuildBranchHook;
import io.harness.app.beans.entities.CIBuildPRHook;
import io.harness.app.beans.entities.CIBuildPipeline;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public class CIBuildResponseDTO {
  private Long id;
  private String status;
  private String errorMessage;
  private long startTime;
  private long endTime;
  private CIBuildPipeline pipeline;
  private String triggerType;
  private String event;
  private CIBuildAuthor author;
  private CIBuildBranchHook branch;
  private CIBuildPRHook pullRequest;
}
